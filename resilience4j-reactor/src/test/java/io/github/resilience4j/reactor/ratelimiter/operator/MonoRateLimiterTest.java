/*
 * Copyright 2018 Julien Hoarau
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.resilience4j.reactor.ratelimiter.operator;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.time.Duration;

import static org.mockito.BDDMockito.given;

public class MonoRateLimiterTest {

    private RateLimiter rateLimiter;

    @Before
    public void setUp(){
        rateLimiter = Mockito.mock(RateLimiter.class);
    }

    @Test
    public void shouldEmitEvent() {
        given(rateLimiter.acquirePermission(Duration.ZERO)).willReturn(true);

        StepVerifier.create(
                Mono.just("Event")
                        .compose(RateLimiterOperator.of(rateLimiter)))
                .expectNext("Event")
                .verifyComplete();
    }

    @Test
    public void shouldPropagateError() {
        given(rateLimiter.acquirePermission(Duration.ZERO)).willReturn(true);

        StepVerifier.create(
                Mono.error(new IOException("BAM!"))
                        .compose(RateLimiterOperator.of(rateLimiter)))
                .expectSubscription()
                .expectError(IOException.class)
                .verify(Duration.ofSeconds(1));
    }

    @Test
    public void shouldEmitErrorWithBulkheadFullException() {
        given(rateLimiter.acquirePermission(Duration.ZERO)).willReturn(false);

        StepVerifier.create(
                Mono.just("Event")
                        .compose(RateLimiterOperator.of(rateLimiter)))
                .expectSubscription()
                .expectError(RequestNotPermitted.class)
                .verify(Duration.ofSeconds(1));
    }

    @Test
    public void shouldEmitRequestNotPermittedExceptionEvenWhenErrorDuringSubscribe() {
        given(rateLimiter.acquirePermission(Duration.ZERO)).willReturn(false);

        StepVerifier.create(
                Mono.error(new IOException("BAM!"))
                        .compose(RateLimiterOperator.of(rateLimiter)))
                .expectError(RequestNotPermitted.class)
                .verify(Duration.ofSeconds(1));
    }
}