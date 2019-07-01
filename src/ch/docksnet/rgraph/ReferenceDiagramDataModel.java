/*
 * Copyright (C) 2019 Stefan Zeller
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ch.docksnet.rgraph;

import ch.docksnet.rgraph.method.FQN;
import ch.docksnet.rgraph.method.LCOMConverter;
import ch.docksnet.rgraph.method.ReferenceNode;
import ch.docksnet.rgraph.method.SourceTargetPair;
import ch.docksnet.utils.IncrementableSet;
import ch.docksnet.utils.lcom.ClusterAnalyzer;
import ch.docksnet.utils.lcom.LCOMAnalyzerData;
import ch.docksnet.utils.lcom.LCOMNode;
import com.intellij.diagram.DiagramDataModel;
import com.intellij.diagram.DiagramEdge;
import com.intellij.diagram.DiagramNode;
import com.intellij.diagram.DiagramProvider;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.ModificationTracker;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiManager;
import com.intellij.psi.SmartPointerManager;
import com.intellij.psi.SmartPsiElementPointer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static ch.docksnet.rgraph.PsiUtils.getFqn;

public abstract class ReferenceDiagramDataModel extends DiagramDataModel<PsiElement> {

    private final Map<FQN, SmartPsiElementPointer<PsiElement>> elementsAddedByUser = new HashMap();
    private final Map<FQN, SmartPsiElementPointer<PsiElement>> elementsRemovedByUser = new HashMap();

    private final Collection<DiagramNode<PsiElement>> nodes = new HashSet<>();
    private final Map<PsiElement, DiagramNode<PsiElement>> nodesPool = new HashMap<>();
    private final Collection<DiagramEdge<PsiElement>> edges = new HashSet<>();

    private final SmartPointerManager spManager;

    private long currentClusterCount = 0;

    public ReferenceDiagramDataModel(Project project, DiagramProvider<PsiElement> provider) {
        super(project, provider);
        this.spManager = SmartPointerManager.getInstance(getProject());
    }

    @NotNull
    @Override
    public Collection<? extends DiagramNode<PsiElement>> getNodes() {
        if (this.nodes == null) {
            throw new IllegalStateException("@NotNull method %s.%s must not return null");
        } else {
            return this.nodes;
        }
    }

    @NotNull
    @Override
    public Collection<? extends DiagramEdge<PsiElement>> getEdges() {
        if (this.edges == null) {
            throw new IllegalStateException(String.format("@NotNull method %s.%s must not return null",
                    new Object[]{"com/intellij/uml/java/JavaUmlDataModel", "getEdges"}));
        } else {
            return this.edges;
        }
    }

    @Override
    public void refreshDataModel() {
        clearAll();
        updateDataModel();
    }

    protected void refresh() {
        analyzeLcom4();
    }

    private Set<PsiElement> getElements() {
        Set<PsiElement> result = new HashSet<>();

        for (SmartPsiElementPointer<PsiElement> psiElementPointer : this.elementsAddedByUser.values()) {
            PsiElement element = psiElementPointer.getElement();
            result.add(element);
        }

        for (SmartPsiElementPointer<PsiElement> psiElementPointer : this.elementsRemovedByUser.values()) {
            PsiElement element = psiElementPointer.getElement();
            result.remove(element);
        }

        return result;
    }

    protected void updateDataModel() {
        DiagramProvider provider = getBuilder().getProvider();
        Set<PsiElement> elements = getElements();

        for (PsiElement element : elements) {
            if (isAllowedToShow(element)) {
                this.nodes.add(getReferenceNode(provider, element));
            }
        }

        IncrementableSet<SourceTargetPair> relationships = resolveRelationships();
        for (Map.Entry<SourceTargetPair, Long> sourceTargetPair : relationships.elements()) {
            SourceTargetPair key = sourceTargetPair.getKey();
            DiagramNode<PsiElement> source = findNode(key.getSource());
            DiagramNode<PsiElement> target = findNode(key.getTarget());
            if (source != null && target != null) {
                this.edges.add(toEdge(source, target, sourceTargetPair.getValue()));
            }
        }
    }

    protected void clearAll() {
        this.nodes.clear();
        this.edges.clear();
        this.elementsRemovedByUser.clear();
    }

    protected SmartPsiElementPointer<PsiElement> createSmartPsiElementPointer(PsiElement psiElement) {
        return this.spManager.createSmartPsiElementPointer(psiElement);
    }

    @NotNull
    private ReferenceNode getReferenceNode(DiagramProvider provider, PsiElement element) {
        if (this.nodesPool.containsKey(element)) {
            return (ReferenceNode) this.nodesPool.get(element);
        }
        ReferenceNode node = new ReferenceNode(element, provider);
        this.nodesPool.put(element, node);
        return node;
    }

    @Override
    public boolean hasElement(PsiElement element) {
        return findNode(element) != null;
    }

    /**
     * @param psiElement
     * @return {@code true} if {@code nodes} contains {@code psiElement}.
     */
    @Nullable
    private DiagramNode<PsiElement> findNode(PsiElement psiElement) {
        Iterator ptr = (new ArrayList(this.nodes)).iterator();

        while (ptr.hasNext()) {
            DiagramNode node = (DiagramNode) ptr.next();
            FQN fqn = PsiUtils.getFqn((PsiElement) node.getIdentifyingElement());
            if (fqn != null && fqn.equals(PsiUtils.getFqn(psiElement))) {
                return node;
            }
        }
        return null;
    }

    @Override
    public boolean isPsiListener() {
        return true;
    }

    public void removeMarkedNodes() {
        List<ReferenceNode> toRemove = new ArrayList<>();
        for (DiagramNode<PsiElement> myNode : this.nodes) {
            if (myNode instanceof ReferenceNode) {
                if (((ReferenceNode) myNode).isMarked()) {
                    toRemove.add((ReferenceNode) myNode);
                    ((ReferenceNode) myNode).switchMarked();
                }
            }
        }
        Iterator<ReferenceNode> iterator = toRemove.iterator();
        while (iterator.hasNext()) {
            ReferenceNode next = iterator.next();
            removeElement((PsiElement) next.getIdentifyingElement());
        }
        analyzeLcom4();
    }

    public void isolateMarkedNodes() {
        List<ReferenceNode> toRemove = new ArrayList<>();
        for (DiagramNode<PsiElement> myNode : this.nodes) {
            if (myNode instanceof ReferenceNode) {
                if (!((ReferenceNode) myNode).isMarked()) {
                    toRemove.add((ReferenceNode) myNode);
                } else {
                    ((ReferenceNode) myNode).switchMarked();
                }
            }
        }
        Iterator<ReferenceNode> iterator = toRemove.iterator();
        while (iterator.hasNext()) {
            ReferenceNode next = iterator.next();
            removeElement((PsiElement) next.getIdentifyingElement());
        }
        analyzeLcom4();
    }

    public void unmarkAllNodes() {
        for (DiagramNode<PsiElement> myNode : this.nodes) {
            if (myNode instanceof ReferenceNode) {
                ((ReferenceNode) myNode).unsetMarked();
            }
        }
    }

    private void analyzeLcom4() {
        LCOMConverter lcomConverter = new LCOMConverter();
        Collection<LCOMNode> lcom4Nodes = lcomConverter.convert(getNodes(), getEdges());
        LCOMAnalyzerData lcomAnalyzerData = new LCOMAnalyzerData(lcom4Nodes);
        ClusterAnalyzer clusterAnalyzer = new ClusterAnalyzer(lcomAnalyzerData);
        this.currentClusterCount = clusterAnalyzer.countCluster();
    }

    private void removeElement(PsiElement element) {
        DiagramNode node = findNode(element);
        if (node == null) {
            this.elementsAddedByUser.remove(PsiUtils.getFqn(element));
        } else {
            PsiElement toRemove = (PsiElement) node.getIdentifyingElement();
            this.nodes.remove(node);
            this.elementsRemovedByUser.put(PsiUtils.getFqn(element), createSmartPsiElementPointer(toRemove));
            this.elementsAddedByUser.remove(PsiUtils.getFqn(element));
            removeAllEdgesFromOrTo(node);
        }
    }

    private void removeAllEdgesFromOrTo(DiagramNode<PsiElement> node) {
        FQN removedNode = PsiUtils.getFqn(node.getIdentifyingElement());
        Set<DiagramEdge<PsiElement>> toRemove = new HashSet<>();
        for (DiagramEdge<PsiElement> myEdge : this.edges) {
            if (PsiUtils.getFqn(myEdge.getSource().getIdentifyingElement()).equals(removedNode)) {
                toRemove.add(myEdge);
            } else if (PsiUtils.getFqn(myEdge.getTarget().getIdentifyingElement()).equals(removedNode)) {
                toRemove.add(myEdge);
            }
        }
        this.edges.removeAll(toRemove);
    }

    @Override
    public void removeNode(DiagramNode<PsiElement> node) {
        removeElement((PsiElement) node.getIdentifyingElement());
        analyzeLcom4();
    }

    @Override
    public void dispose() {
    }

    protected void addUserElement(PsiElement child) {
        this.elementsAddedByUser.put(getFqn(child), createSmartPsiElementPointer(child));
    }

    @Nullable
    @Override
    public DiagramNode<PsiElement> addElement(PsiElement psiElement) {
        return null;
    }

    protected abstract boolean isAllowedToShow(PsiElement element);

    protected abstract IncrementableSet<SourceTargetPair> resolveRelationships();

    @NotNull
    @Override
    public ModificationTracker getModificationTracker() {
        return PsiManager.getInstance(getProject()).getModificationTracker();
    }

    public long getCurrentClusterCount() {
        return this.currentClusterCount;
    }

    @Nullable
    protected abstract DiagramEdge<PsiElement> toEdge(@NotNull DiagramNode<PsiElement> from,
                                                      @NotNull DiagramNode<PsiElement> to,
                                                      Long value);
}
