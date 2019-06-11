/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.optaplanner.springboottaskassigning.solver;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.optaplanner.core.api.score.Score;
import org.optaplanner.springboottaskassigning.domain.TaskAssigningSolution;
import org.optaplanner.springboottaskassigning.utils.TaskAssigningGenerator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DefaultSolverManagerTest {

    private SolverManager<TaskAssigningSolution> solverManager;
    private Long tenantId;
    private CountDownLatch solutionChangedLatch;
    private CountDownLatch solvingEndedLatch;

    @Before
    public void setup() {
        solverManager = new DefaultSolverManager<>();
        tenantId = 0L;
        solutionChangedLatch = new CountDownLatch(1);
        solvingEndedLatch = new CountDownLatch(1);
    }

    @After
    public void tearDown() throws InterruptedException {
        solverManager.shutdown();
    }

    @Test
    public void basicUsageOfSolverManagerWithOneProblem() throws InterruptedException {
        TaskAssigningSolution problem =
                new TaskAssigningGenerator(tenantId).createTaskAssigningSolution(24, 4);

        solverManager.solve(tenantId, problem,
                taskAssigningSolution -> solutionChangedLatch.countDown(),
                taskAssigningSolution -> solvingEndedLatch.countDown());
        solutionChangedLatch.await(60, TimeUnit.SECONDS);

        assertEquals(solverManager.getSolverStatus(tenantId), SolverStatus.SOLVING);

        TaskAssigningSolution solution = solverManager.getBestSolution(tenantId);
        assertEquals(solution.getTenantId(), tenantId);

        Score score = solverManager.getBestScore(tenantId);
        assertTrue(score.isSolutionInitialized());
    }

    @Test
    public void onBestSolutionChangeAndOnSolutionEnded() throws InterruptedException {
        TaskAssigningSolution problem =
                new TaskAssigningGenerator(tenantId).createTaskAssigningSolution(1, 1);
        solverManager.solve(tenantId, problem,
                taskAssigningSolution -> solutionChangedLatch.countDown(),
                taskAssigningSolution -> solvingEndedLatch.countDown());
        solutionChangedLatch.await(60, TimeUnit.SECONDS);
        assertEquals(SolverStatus.SOLVING, solverManager.getSolverStatus(tenantId));
        solvingEndedLatch.await(60, TimeUnit.SECONDS);
        assertEquals(SolverStatus.STOPPED, solverManager.getSolverStatus(tenantId));
    }
}