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

import java.util.function.Consumer;

import org.optaplanner.core.api.score.Score;
import org.optaplanner.core.api.solver.Solver;
import org.optaplanner.core.api.solver.event.SolverEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SolverTask<Solution_> implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(SolverTask.class);

    private final Comparable<?> tenantId;
    private Solver<Solution_> solver;
    private Solution_ planningProblem;
    private Consumer<Solution_> onSolvingEnded;

    public SolverTask(Comparable<?> tenantId, Solver<Solution_> solver, Solution_ planningProblem,
                      Consumer<Solution_> onSolvingEnded) {
        this.tenantId = tenantId;
        this.solver = solver;
        this.planningProblem = planningProblem;
        this.onSolvingEnded = onSolvingEnded;
    }

    @Override
    public void run() {
        try {
            logger.info("Running solverTask for tenantId ({}).", tenantId);
            solver.solve(planningProblem);
            if (onSolvingEnded != null) {
                onSolvingEnded.accept(solver.getBestSolution());
            }
            logger.info("Done");
        } catch (Exception e) {
            // TODO: better handling of exception (using Callable? / ExtendedExecuter with afterExecute() method)
            // TODO propagate exception to SolverManager so it restarts solving for this tenant
            e.printStackTrace();
            Thread.currentThread().interrupt();
        }
    }

    public Comparable<?> getTenantId() {
        return tenantId;
    }

    public Solution_ getBestSolution() {
        return solver.getBestSolution();
    }

    public Score getBestScore() {
        return solver.getBestScore();
    }

    public SolverStatus getSolverStatus() {
        if (solver.isTerminateEarly()) {
            return SolverStatus.TERMINATING_EARLY;
        } else if (solver.isSolving()) {
            return SolverStatus.SOLVING;
        } else {
            return SolverStatus.STOPPED;
        }
    }

    public void addEventListener(SolverEventListener<Solution_> eventListener) {
        solver.addEventListener(eventListener);
    }
}