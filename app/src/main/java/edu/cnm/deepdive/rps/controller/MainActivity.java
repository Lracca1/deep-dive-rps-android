package edu.cnm.deepdive.rps.controller;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import edu.cnm.deepdive.rps.R;
import edu.cnm.deepdive.rps.model.Terrain;
import edu.cnm.deepdive.rps.view.TerrainView;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

  private static final int TERRAIN_SIZE = 75;
  private static final int MAX_SLEEP = 10;
  private static final int ITERATIONS_PER_TICK = 100;
  private static final int MIXING_THRESHOLD = 10;
  private static final int PAIRS_TO_MIX = 8;

  private MenuItem startItem;
  private MenuItem stopItem;
  private MenuItem resetItem;
  private SeekBar speedSlider;
  private SeekBar mixingSlider;
  private TextView iterationCount;
  private boolean running;
  private Terrain terrain;
  private TerrainView terrainView;
  private final Object lock = new Object();
  private int mixingLevel;
  private int sleepInterval;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    setupControls();
    setupTerrain();
    stop();
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

  private void setupTerrain() {
    terrain = new Terrain(TERRAIN_SIZE, new Random());
    terrainView = findViewById(R.id.terrain_view);
    terrainView.setCells(terrain.getCells());
    draw();
  }

  private void start() {
    running = true;
    invalidateOptionsMenu();
    // TODO Create and start runner thread.
  }

  private void stop() {
    running = false;
    // TODO Set runner thread to null
    invalidateOptionsMenu();
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

}










