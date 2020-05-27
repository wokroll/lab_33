package com.example.lab33;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Arrays;
import java.util.Random;

public class MainActivity extends AppCompatActivity {
    private final static Random RANDOM = new Random();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void findRoots(View view) {
        String inputA = ((EditText) findViewById(R.id.lab33A)).getText().toString();
        String inputB = ((EditText) findViewById(R.id.lab33B)).getText().toString();
        String inputC = ((EditText) findViewById(R.id.lab33C)).getText().toString();
        String inputD = ((EditText) findViewById(R.id.lab33D)).getText().toString();
        String inputY = ((EditText) findViewById(R.id.lab33Y)).getText().toString();
        String inputMutationPercent = ((EditText) findViewById(R.id.lab33MutationPercent))
                .getText().toString();

        if (inputA.isEmpty() || inputB.isEmpty() || inputC.isEmpty()
                || inputD.isEmpty() || inputY.isEmpty() || inputMutationPercent.isEmpty()) {
            Toast.makeText(this, "Not all numbers specified!",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        double mutationPercent = Double.parseDouble(inputMutationPercent);
        if (mutationPercent < 0 || mutationPercent > 1) {
            Toast.makeText(this, "Mutation percent should be from 0 to 1!",
                    Toast.LENGTH_SHORT).show();
        }

        int a = Integer.parseInt(inputA);
        int b = Integer.parseInt(inputB);
        int c = Integer.parseInt(inputC);
        int d = Integer.parseInt(inputD);
        int y = Integer.parseInt(inputY);

        long time = System.nanoTime();
        int[] xs = findSolution(a, b, c, d, y, mutationPercent);
        time = System.nanoTime() - time;

        TextView textViewRoots = findViewById(R.id.lab33Roots);
        textViewRoots.setText("Roots: " + Arrays.toString(xs));

        TextView textViewTime = findViewById(R.id.lab33Time);
        textViewTime.setText("Time (sec): " + time / 1_000_000_000.0);
    }

    private int[][] generateStartingPopulation(int y) {
        int[][] population = new int[5 + RANDOM.nextInt(y - 4)][4];
        for (int i = 0; i < population.length; i++) {
            for (int j = 0; j < population[0].length; j++) {
                population[i][j] = RANDOM.nextInt(y / 2);
            }
        }
        return population;
    }

    private int[] findSolution(int a, int b, int c, int d, int y, double mutationPercent) {
        int[][] population = generateStartingPopulation(y);
        int[] abcd = {a, b, c, d};
        int index;
        int[] deltas;
        while (true) {
            deltas = fitnessFunction(population, abcd, y);
            if ((index = argZero(deltas)) != -1) {
                break;
            } else {
                double meanSurvOld = findMean(survivalLikelihood(deltas));
                int[][] newPopulation = newPopulation(deltas, population);
                if (meanSurvOld <
                        findMean(survivalLikelihood(fitnessFunction(newPopulation, abcd, y)))
                ) {
                    population = newPopulation;
                } else {
                    randomMutation(population, y, mutationPercent);
                }
            }
        }
        return population[index];
    }

    private void randomMutation(int[][] population, int y, double mutationPercent) {
        if (RANDOM.nextDouble() < mutationPercent) {
            for (int i = 0; i < population.length; i++) {
                int randIndex = RANDOM.nextInt(population[0].length);
                population[i][randIndex] = RANDOM.nextInt(y);
            }
        }
    }

    private int[][] newPopulation(int[] deltas, int[][] population) {
        double[] survProb = survivalLikelihood(deltas);
        int[][] parentPairs = createPairs(survProb, population);
        return crossOverPairs(parentPairs, population);
    }

    private int[][] crossOverPairs(int[][] parentPairs, int[][] population) {
        int[][] newPopulation = new int[population.length][4];
        for (int i = 0; i < population.length; i++) {
            newPopulation[i] = crossOver(population[parentPairs[i][0]],
                    population[parentPairs[i][1]]);
        }
        return newPopulation;
    }

    private int[] crossOver(int[] pair1, int[] pair2) {
        int bound = 1 + RANDOM.nextInt(3);
        int[] child = new int[pair1.length];
        for (int i = 0; i < child.length; i++) {
            if (i < bound) {
                child[i] = pair1[i];
            } else {
                child[i] = pair2[i];
            }
        }
        return child;
    }

    private int[][] createPairs(double[] survProb, int[][] population) {
        int[][] pairs = new int[population.length][2];
        int candidatesNum = survProb.length / 2;
        int[] parents = new int[candidatesNum];
        int maxIndex;
        double max;
        for(int i = 0; i < candidatesNum; i++) {
            max = survProb[0];
            maxIndex = 0;
            for (int j = 1; j < survProb.length; j++) {
                if (survProb[j] > max) {
                    max = survProb[j];
                    maxIndex = j;
                }
            }
            survProb[maxIndex] = -1;
            parents[i] = maxIndex;
        }

        for (int i = 0; i < pairs.length;) {
            pairs[i][0] = parents[RANDOM.nextInt(parents.length)];
            pairs[i][1] = parents[RANDOM.nextInt(parents.length)];
            if (pairs[i][0] != pairs[i][1]) {
                i++;
            }
        }
        return pairs;
    }

    private double[] survivalLikelihood(int[] deltas) {
        double cummProb = 0;
        double[] surv = new double[deltas.length];
        for (int delta : deltas) {
            cummProb += (double) 1 / delta;
        }
        for (int i = 0; i < deltas.length; i++) {
            surv[i] = ((double) 1 / deltas[i]) / cummProb;
        }
        return surv;
    }

    private int[] fitnessFunction(int[][] population, int[] abcd, int y) {
        int[] deltas = new int[population.length];
        for (int i = 0; i < population.length; i++) {
            for (int j = 0; j < population[0].length; j++) {
                deltas[i] += population[i][j] * abcd[j];
            }
            deltas[i] = Math.abs(deltas[i] - y);
        }
        return deltas;
    }

    private int argZero(int[] array) {
        int index = -1;
        for (int i = 0; i < array.length; i++) {
            if (array[i] == 0) {
                index = i;
                break;
            }
        }
        return index;
    }

    private double findMean(double[] array) {
        double mean = 0;
        for (double v : array) {
            mean += v;
        }
        mean /= array.length;
        return mean;
    }

}