/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.tomee.sheldon.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Scratch {

    /**
     * Will be pretty complicated for the connector scenario.  Have to shelve for now
     * Each invocation will need to be done in an appropriately setup thread that has
     * the correct security and CDI context thread locals.
     */
    public static void main(String[] args) throws Exception {
        final ExecutorService executorService = Executors.newFixedThreadPool(10);


        Section section;

        // RED ----------------------------------------------------------------

        final PipedOutputStream redPipe = new PipedOutputStream();
        final Future<?> red = executorService.submit(new BottlesOfBeer(System.in, new PrintStream(redPipe)));
        section = new Section(null, redPipe, red);

        // GREEN --------------------------------------------------------------

        final PipedOutputStream greenPipe = new PipedOutputStream();
        final Future<?> green = executorService.submit(new ImportantNumbers(new PipedInputStream(redPipe), new PrintStream(greenPipe)));
        section = new Section(section, greenPipe, green);

        // GREEN --------------------------------------------------------------

        final PipedOutputStream yellowPipe = new PipedOutputStream();
        final Future<?> yellow = executorService.submit(new Translator(new PipedInputStream(greenPipe), new PrintStream(yellowPipe)));
        section = new Section(section, yellowPipe, yellow);

        final Future<?> blue = executorService.submit(new ToUpperCase(new PipedInputStream(yellowPipe), System.out));
        section = new Section(section, null, blue);

        section.complete();

        executorService.shutdown();
    }

    public void iHateThisCheckStyleCheck() {
    }

    public static class Section {
        private final Section previous;
        private final PipedOutputStream pipe;
        private final Future<?> future;

        public Section(Section previous, PipedOutputStream pipe, Future<?> future) {
            this.previous = previous;
            this.pipe = pipe;
            this.future = future;
        }

        public void complete() throws ExecutionException, InterruptedException, IOException {
            if (previous != null) previous.complete();

            future.get();
            if (pipe instanceof PipedOutputStream) {
                PipedOutputStream pipedOutputStream = (PipedOutputStream) pipe;
                pipedOutputStream.close();
            }
        }
    }

    private static class BottlesOfBeer implements Runnable {
        private final InputStream fromPrevious;
        private final PrintStream toNext;

        public BottlesOfBeer(InputStream fromPrevious, PrintStream toNext) {
            this.fromPrevious = fromPrevious;
            this.toNext = toNext;
        }

        @Override
        public void run() {
            for (int i = 99; i > 0; i--) {
                toNext.printf("%s bottles of beer on the wall%n", i);
                System.out.printf("==%s bottles of beer on the wall%n", i);
            }
        }
    }

    private static class ToUpperCase implements Runnable {
        private final InputStream fromPrevious;
        private final PrintStream toNext;

        public ToUpperCase(InputStream fromPrevious, PrintStream toNext) {
            this.fromPrevious = fromPrevious;
            this.toNext = toNext;
        }

        @Override
        public void run() {
            try {
                int i;
                while ((i = fromPrevious.read()) != -1) {
                    toNext.write(Character.toUpperCase(i));
                }
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }
    }

    private static class ImportantNumbers implements Runnable {
        private final InputStream fromPrevious;
        private final PrintStream toNext;

        public ImportantNumbers(InputStream fromPrevious, PrintStream toNext) {
            this.fromPrevious = fromPrevious;
            this.toNext = toNext;
        }

        @Override
        public void run() {
            try {
                final BufferedReader reader = new BufferedReader(new InputStreamReader(fromPrevious));
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains("0")) {
                        toNext.println(line + "!!!!!");
                    } else {
                        toNext.println(line);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }
    }

    private static class Translator implements Runnable {
        private final InputStream fromPrevious;
        private final PrintStream toNext;

        public Translator(InputStream fromPrevious, PrintStream toNext) {
            this.fromPrevious = fromPrevious;
            this.toNext = toNext;
        }

        @Override
        public void run() {
            try {
                final BufferedReader reader = new BufferedReader(new InputStreamReader(fromPrevious));
                String line;
                while ((line = reader.readLine()) != null) {
                    toNext.println(line.replace("beer", "cerveza"));
                }
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }
    }
}
