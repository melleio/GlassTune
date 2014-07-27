package com.glasstune.activities;

import com.glasstune.R;
import com.glasstune.pitch.IPitchDetectorCallback;
import com.glasstune.pitch.PitchDetector;
import com.glasstune.tone.Note;
import com.glasstune.utils.NoteCalculator;
import com.google.android.glass.app.Card;
import com.google.android.glass.media.Sounds;
import com.google.android.glass.widget.CardScrollAdapter;
import com.google.android.glass.widget.CardScrollView;

import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsoluteLayout;
import android.widget.AdapterView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import be.hogent.tarsos.dsp.AudioEvent;
import be.hogent.tarsos.dsp.MicrophoneAudioDispatcher;
import be.hogent.tarsos.dsp.pitch.PitchDetectionHandler;
import be.hogent.tarsos.dsp.pitch.PitchDetectionResult;
import be.hogent.tarsos.dsp.pitch.PitchProcessor;

/**
 * An {@link Activity} showing a tuggable "Hello World!" card.
 * <p>
 * The main content view is composed of a one-card {@link CardScrollView} that provides tugging
 * feedback to the user when swipe gestures are detected.
 * If your Glassware intends to intercept swipe gestures, you should set the content view directly
 * and use a {@link com.google.android.glass.touchpad.GestureDetector}.
 * @see <a href="https://developers.google.com/glass/develop/gdk/touch">GDK Developer Guide</a>
 */
public class TuneGuitarActivity extends Activity implements PitchDetectionHandler {

    /** {@link CardScrollView} to use as the main content view. */
    private CardScrollView mCardScroller;

    /** "Hello World!" {@link View} generated by {@link #buildView()}. */
    private View mView;
    private Thread _pitchThread;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        mView = buildView();

        mCardScroller = new CardScrollView(this);
        mCardScroller.setAdapter(new CardScrollAdapter() {
            @Override
            public int getCount() {
                return 1;
            }

            @Override
            public Object getItem(int position) {
                return mView;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                return mView;
            }

            @Override
            public int getPosition(Object item) {
                if (mView.equals(item)) {
                    return 0;
                }
                return AdapterView.INVALID_POSITION;
            }
        });
        // Handle the TAP event.
        mCardScroller.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                openOptionsMenu();
            }
        });
        setContentView(mCardScroller);

        int sampleRate = 44100;
        int bufferSize = 1024;
        int overlap = 512;

        MicrophoneAudioDispatcher dispatcher = new MicrophoneAudioDispatcher(sampleRate,bufferSize,overlap);
        dispatcher.addAudioProcessor(new PitchProcessor(PitchProcessor.PitchEstimationAlgorithm.FFT_YIN, sampleRate, bufferSize, this));
        new Thread(dispatcher,"Audio dispatching").start();

        //PitchDetector pitch = new PitchDetector(this);
        //_pitchThread = new Thread(pitch);
        //_pitchThread.start();
    }

    @Override
    public void handlePitch(PitchDetectionResult pitchDetectionResult,AudioEvent audioEvent) {
        if(pitchDetectionResult.getPitch() != -1){
            double timeStamp = audioEvent.getTimeStamp();
            final float pitch = pitchDetectionResult.getPitch();
            float probability = pitchDetectionResult.getProbability();
            double rms = audioEvent.getRMS() * 100;
            String message = String.format("Pitch detected at %.2fs: %.2fHz ( %.2f probability, RMS: %.5f )\n", timeStamp,pitch,probability,rms);
            Log.d("PITCH",message);

            this.runOnUiThread(new Runnable() {
                public void run() {
                    setDisplayForFrequency(pitch);
                }
            });

        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mCardScroller.activate();
    }

    @Override
    protected void onPause() {
        //_pitchThread.interrupt();
        //mCardScroller.deactivate();
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.tuner_menu,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.dismiss_menu_item:
                _pitchThread.interrupt();
                mCardScroller.deactivate();

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void setDisplayForFrequency(double frequency) {
        Note mainNote = Note.getNearestNote(frequency);
        Note sharpNote = Note.getNextNote(mainNote);
        Note flatNote = Note.getPreviousNote(mainNote);

        TextView mainNoteText = (TextView)findViewById(R.id.tune_view_main_note);
        mainNoteText.setText(mainNote.toString());

        TextView flatNoteText = (TextView)findViewById(R.id.tune_view_flat_note);
        flatNoteText.setText(flatNote.toString());

        TextView sharpNoteText = (TextView)findViewById(R.id.tune_view_sharp_note);
        sharpNoteText.setText(sharpNote.toString());

        View pitchBar = (View)findViewById(R.id.tune_view_current_pitch);
        double left = NoteCalculator.getPitchBarPercentage(frequency);
        double leftDP = left * (double)640;

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(pitchBar.getWidth(),pitchBar.getHeight());
        params.setMargins((int)leftDP,0,0,0);
        pitchBar.setLayoutParams(params);
    }

    /**
     * Builds a Glass styled "Hello World!" view using the {@link Card} class.
     */
    private View buildView() {
        return getLayoutInflater().inflate(R.layout.tune_view,null);
    }

}
