/*
 * Copyright (C) 2015 Vincenzo Prignano
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.JPGSolver;

import com.beust.jcommander.Parameter;

import java.util.ArrayList;
import java.util.List;

public class CommandLineArgs {
    @Parameter(names = {"--justHeat", "-jh"}, description = "Output time only")
    public boolean justHeat = false;

    @Parameter(description = "Parameters")
    public List<String> params = new ArrayList<String>();

    @Parameter(names = "--parallel, -p", description = "Parallelized Attractor")
    public boolean parallel = false;

    @Parameter(names = "--iterative, -i", description = "Explicited Stack")
    public boolean iterative = false;

    @Parameter(names = {"--tests", "-t"}, description = "Explicited Stack")
    public boolean tests = false;

    @Parameter(names = {"--smallpm", "-spm"}, description = "Small Progress Measures algorithm")
    public boolean smallpm = false;

    @Parameter(names = {"--zielonka", "-z"}, description = "Zielonka recursive algorithm")
    public boolean zielonka = false;

    @Parameter(names = {"--parallelZielonka", "-pz"}, description = "Parallelised Zielonka")
    public boolean parallelZielonka = false;

    @Parameter(names = {"--help", "-h"}, description = "Show Help")
    public boolean help = false;
}
