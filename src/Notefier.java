import java.awt.GridLayout;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.InvocationTargetException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.TargetDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.io.jvm.JVMAudioInputStream;
import be.tarsos.dsp.pitch.PitchDetectionHandler;
import be.tarsos.dsp.pitch.PitchDetectionResult;
import be.tarsos.dsp.pitch.PitchProcessor;
import be.tarsos.dsp.pitch.PitchProcessor.PitchEstimationAlgorithm;

import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.sound.sampled.Mixer;
import javax.swing.JButton;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.pitch.PitchProcessor.PitchEstimationAlgorithm;

import javax.swing.JTextArea;
import java.awt.Color;

import javax.sound.sampled.AudioSystem;

public class Notefier implements PitchDetectionHandler {
	
	public static final String MIXER_INFO_ID = "Default Audio Device, version Unknown Version";

	private JFrame frame;
	private JTextArea textArea;
	private AudioDispatcher dispatcher;
	private Mixer mixer;
	
	private PitchEstimationAlgorithm algo;
	

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					Notefier window = new Notefier();
					window.frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public Notefier() {
		//create the GUI
		initialize();
	}
	
	private void setMixer() throws LineUnavailableException {
		algo = PitchEstimationAlgorithm.YIN;
		//Search for correct mixer info
		for(Mixer.Info info : AudioSystem.getMixerInfo()){
			if (info.toString().equals(MIXER_INFO_ID)) {
				mixer = AudioSystem.getMixer(info);
			}
		}
		if(dispatcher!= null){
			dispatcher.stop();
		}
		
		//float sampleRate = 1;
		float sampleRate = 44100;
		int bufferSize = 1024;
		int overlap = 0;
		
		textArea.append("Started listening with " + mixer.toString() + "\n");

		final AudioFormat format = new AudioFormat(sampleRate, 16, 1, true,
				true);
		final DataLine.Info dataLineInfo = new DataLine.Info(
				TargetDataLine.class, format);
		TargetDataLine line = null;
		line = (TargetDataLine) mixer.getLine(dataLineInfo);
		final int numberOfSamples = bufferSize;
		line.open(format, numberOfSamples);
		line.start();
		final AudioInputStream stream = new AudioInputStream(line);

		JVMAudioInputStream audioStream = new JVMAudioInputStream(stream);
		// create a new dispatcher
		dispatcher = new AudioDispatcher(audioStream, bufferSize,
				overlap);

		// add a processor
		dispatcher.addAudioProcessor(new PitchProcessor(algo, sampleRate, bufferSize, this));
		
		new Thread(dispatcher,"Audio dispatching").start();
	}
	
	public String getNote(float pitch) {
		if (pitch > 300) return "A";
		return "B";
	}
	

	@Override
	public void handlePitch(PitchDetectionResult pitchDetectionResult,AudioEvent audioEvent) {
		if(pitchDetectionResult.getPitch() != -1){
			double timeStamp = audioEvent.getTimeStamp();
			float pitch = pitchDetectionResult.getPitch();
			float probability = pitchDetectionResult.getProbability();
			double rms = audioEvent.getRMS() * 100;
			String message = String.format("Pitch detected at %.2fs: %.2fHz ( %.2f probability, RMS: %.5f ) The note is: %s\n", timeStamp,pitch,probability,rms, getNote(pitch));
			textArea.append(message);
			textArea.setCaretPosition(textArea.getDocument().getLength());
		}
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frame = new JFrame();
		frame.getContentPane().setBackground(new Color(0, 100, 0));
		frame.setBounds(100, 100, 646, 521);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().setLayout(null);
		
		JButton btnStartNoteifying = new JButton("Start");
		btnStartNoteifying.setBackground(new Color(255, 255, 255));
		btnStartNoteifying.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					setMixer();
				} catch (LineUnavailableException e1) {
					e1.printStackTrace();
				}
			}
		});
		btnStartNoteifying.setBounds(274, 38, 117, 29);
		frame.getContentPane().add(btnStartNoteifying);
		
		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setBounds(35, 105, 586, 311);
		frame.getContentPane().add(scrollPane);
		
		textArea = new JTextArea();
		textArea.setEditable(false);
		scrollPane.setViewportView(textArea);
		textArea.setBackground(new Color(255, 255, 255));
	}
}
