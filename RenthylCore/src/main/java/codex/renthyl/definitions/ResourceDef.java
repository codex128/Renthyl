/*
 * Copyright (c) 2024, codex
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the copyright holder nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package codex.renthyl.definitions;

import codex.renthyl.resources.Disposer;

/**
 * Creates, evaluates, and disposes resources.
 * 
 * @author codex
 * @param <T>
 */
public interface ResourceDef <T> extends Disposer<T> {
    
    /**
     * Creates a new resources exactly meeting the definition's criteria
     * as defined by the implementor.
     * 
     * @return 
     */
    T createResource();
    
    /**
     * Checks if the resource can be allocated.
     *
     * <p>The returned score indicates how suited the resource is to criteria of the definition as defined
     * by the implementor, where lower values represent better matches. Once all resources have been evaluated
     * (or another terminating condition occurs), the resource with the lowest score is
     * {@link #conformResource(Object) conformed} and used.</p>
     *
     * A score of <p>{@code 0f} or lower means the resource is perfect for the current parameters
     * (thus terminating further evaluations). All scores {@code 0f} and lower are therefore considered
     * equal. {@code null} means the resource is completely unsuited, and should not be considered
     * further.</p>
     * 
     * @param resource
     * @return evaluation score of the resource
     */
    Float evaluateResource(Object resource);

    /**
     * Configures the resource exactly to the definition's criteria.
     *
     * @param resource conformed resource
     */
    T conformResource(Object resource);

    /**
     * Tests if the evaluation score is perfect. A perfect score is non-null and less than or equal to {@code 0f},
     * and implies that evaluating further resources is unnecessary.
     *
     * @param score
     * @return
     */
    static boolean isPerfectEvaluation(Float score) {
        return score != null && score <= 0f;
    }

    /**
     * Returns the sum of {@code score} and {@code add} if {@code score} is not null.
     * Otherwise returns null.
     *
     * @param score
     * @param add
     * @return
     */
    static Float addEval(Float score, float add) {
        if (score == null) return null;
        else return score + add;
    }
    
}
