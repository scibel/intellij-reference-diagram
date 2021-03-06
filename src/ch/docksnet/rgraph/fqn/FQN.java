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

package ch.docksnet.rgraph.fqn;

import java.util.Objects;

public abstract class FQN {
    abstract public String getFQN();

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof FQN)) {
            return false;
        }
        FQN otherFqn = (FQN) other;
        return Objects.equals(this.getFQN(), otherFqn.getFQN());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getFQN());
    }
}
