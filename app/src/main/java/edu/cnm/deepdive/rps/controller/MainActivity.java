package edu.cnm.deepdive.rps.controller;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import edu.cnm.deepdive.rps.R;
import edu.cnm.deepdive.rps.model.Breed;
import edu.cnm.deepdive.rps.model.Terrain;
import edu.cnm.deepdive.rps.view.TerrainView;
import java.util.Arrays;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

  private static final int TERRAIN_SIZE = 75;
  private static final int MAX_SLEEP = 10;
  private static final int ITERATIONS_PER_TICK = 100;
  private static final int MIXING_THRESHOLD = 10;
  private static final int PAIRS_TO_MIX = 8;
  private static final String RUNNING_KEY = "running";
  private static final String ORDINALS_KEY = "ordinals";
  private static final String COUNTS_KEY = "counts";

  private MenuItem startItem;
  private MenuItem stopItem;
  private MenuItem resetItem;
  private SeekBar speedSlider;
  private SeekBar mixingSlider;
  private TextView iterationCount;
  private boolean running;
  private Random rng;
  private Terrain terrain;
  private TerrainView terrainView;
  private int mixingLevel;
  private int sleepInterval;
  private Runner runner;
  private final Object lock = new Object();

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    setupControls();
    setupTerrain(savedInstanceState);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    super.onCreateOptionsMenu(menu);
    getMenuInflater().inflate(R.menu.options, menu);
    startItem = menu.findItem(R.id.action_start);
    stopItem = menu.findItem(R.id.action_stop);
    resetItem = menu.findItem(R.id.action_reset);
    return true;
  }

  @Override
  public boolean onPrepareOptionsMenu(Menu menu) {
    super.onPrepareOptionsMenu(menu);
    startItem.setVisible(!running && !terrain.isAbsorbed());
    stopItem.setVisible(running);
    resetItem.setEnabled(!running);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    boolean handled = true;
    switch (item.getItemId()) {
      case R.id.action_start:
        start();
        break;
      case R.id.action_stop:
        stop();
        break;
      case R.id.action_reset:
        reset();
        break;
      default:
        handled = super.onOptionsItemSelected(item);
    }
    return handled;
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    Breed[][] cells = terrain.getCells();
    byte[] ordinals = new byte[cells.length * cells[0].length]; // Breaks for a jagged array.
    int[] counts = new int[Breed.values().length];
    int width = cells[0].length;
    synchronized (lock) {
      for (int i = 0; i < cells.length; i++) {
        int offset = i * width;
        for (int j = 0; j < cells[i].length; j++) {
          ordinals[offset + j] = (byte) cells[i][j].ordinal();
        }
      }
      counts = Arrays.copyOf(terrain.getCounts(), counts.length);
    }
    outState.putBoolean(RUNNING_KEY, running);
    outState.putByteArray(ORDINALS_KEY, ordinals);
    outState.putIntArray(COUNTS_KEY, counts);
  }

  @Override
  protected void onDestroy() {
    stop();
    super.onDestroy();
  }

  private void setupControls() {
    speedSlider = findViewById(R.id.speed_slider);
    mixingSlider = findViewById(R.id.mixing_slider);
    iterationCount = findViewById(R.id.iteration_count);
    speedSlider.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
      @Override
      public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        sleepInterval = 1 + MAX_SLEEP - speedSlider.getProgress();
      }
      @Override
      public void onStartTrackingTouch(SeekBar seekBar) { }
      @Override
      public void onStopTrackingTouch(SeekBar seekBar) { }
    });
    mixingSlider.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
      @Override
      public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        mixingLevel = mixingSlider.getProgress();
      }
      @Override
      public void onStartTrackingTouch(SeekBar seekBar) { }
      @Override
      public void onStopTrackingTouch(SeekBar seekBar) { }
    });
    sleepInterval = 1 + MAX_SLEEP - speedSlider.getProgress();
    mixingLevel = mixingSlider.getProgress();
  }

  private void setupTerrain(Bundle savedInstanceState) {
    boolean wasRunning;
    byte[] ordinals = null;
    int[] counts = null;
    rng = new Random();
    terrain = new Terrain(TERRAIN_SIZE, rng);
    Breed[][] cells = terrain.getCells();
    if (savedInstanceState != null) {
      wasRunning = savedInstanceState.getBoolean(RUNNING_KEY);
      ordinals = savedInstanceState.getByteArray(ORDINALS_KEY);
      counts = savedInstanceState.getIntArray(COUNTS_KEY);
      Breed[] breeds = Breed.values();
      int width = cells[0].length;
      for (int i = 0; i < cells.length; i++) {
        int offset = i * width;
        for (int j = 0; j < cells[i].length; j++) {
          cells[i][j] = breeds[ordinals[offset + j]];
        }
      }
      System.arraycopy(counts, 0, terrain.getCounts(), 0, counts.length);
    } else {
      wasRunning = false;
      terrain.reset();
    }
    terrainView = findViewById(R.id.terrain_view);
    terrainView.setCells(cells);
    draw();
    if (wasRunning) {
      start();
    } else {
      stop();
    }
  }

  private void start() {
    running = true;
    invalidateOptionsMenu();
    runner = new Runner();
    runner.start();
  }

  private void stop() {
    running = false;
    runner = null;
  }

  private void reset() {
    terrain.reset();
    invalidateOptionsMenu();
    draw();
  }

  private void draw() {
    synchronized (lock) {
      terrainView.invalidate();
      iterationCount.setText(getString(R.string.iterations_format, terrain.getIterations()));
    }
  }

  private class Runner extends Thread {

    @Override
    public void run() {
      int mixingAccumulator = 0;
      while (running) {
        synchronized (lock) {
          terrain.iterate(ITERATIONS_PER_TICK);
          mixingAccumulator += mixingLevel;
          if (mixingAccumulator >= MIXING_THRESHOLD) {
            terrain.mix(PAIRS_TO_MIX);
            mixingAccumulator %= MIXING_THRESHOLD;
          }
        }
        if (!terrainView.isDrawing()) {
          update(false);
        }
        try {
          Thread.sleep(sleepInterval);
        } catch (InterruptedException e) {
          // DO NOTHING; DOESN'T MATTER!
        }
        if (terrain.isAbsorbed()) {
          MainActivity.this.stop();
        }
      }
      update(true);
    }

    private void update(final boolean stopping) {
      runOnUiThread(new Runnable() {
        @Override
        public void run() {
          draw();
          if (stopping) {
            invalidateOptionsMenu();
          }
        }
      });
    }

  }

}















