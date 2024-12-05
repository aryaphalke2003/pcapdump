package com.example.dumper;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.pcap4j.core.*;
import org.pcap4j.packet.Packet;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    private boolean isCapturing = false;  // Shared flag for button state
    private PcapHandle handle;
    private Thread buttonListenerThread;
    private Thread captureThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button captureButton = findViewById(R.id.captureButton);
        TextView statusText = findViewById(R.id.statusText);

        // Start a thread to monitor button clicks
        startButtonListener(captureButton, statusText);
    }

    private void startButtonListener(Button captureButton, TextView statusText) {
        buttonListenerThread = new Thread(() -> {
            captureButton.setOnClickListener(v -> {
                isCapturing = !isCapturing; // Toggle capture state
                if (isCapturing) {
                    runOnUiThread(() -> {
                        statusText.setText("Status: Capturing...");
                        captureButton.setText("Stop Capture");
                    });
                    startCaptureThread(statusText);
                } else {
                    runOnUiThread(() -> {
                        statusText.setText("Status: Capture Stopped");
                        captureButton.setText("Start Capture");
                    });
                    stopCaptureThread();
                }
            });
        });
        buttonListenerThread.start();
    }

    private void startCaptureThread(TextView statusText) {
        captureThread = new Thread(() -> {
            try {
                String interfaceName = "wlan0"; // Replace with your interface
                PcapNetworkInterface nif = Pcaps.getDevByName(interfaceName);
                if (nif == null) {
                    throw new IllegalArgumentException("No such interface: " + interfaceName);
                }

                File pcapFile = new File("/sdcard/dump.pcap");
                handle = nif.openLive(65536, PcapNetworkInterface.PromiscuousMode.PROMISCUOUS, 10);
                PcapDumper dumperHandle = handle.dumpOpen(pcapFile.getAbsolutePath());

                while (isCapturing) {
                    Packet packet = handle.getNextPacketEx();
                    dumperHandle.dump(packet, handle.getTimestamp());
                }

                dumperHandle.close();
                handle.close();

                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Capture saved to: " + pcapFile.getAbsolutePath(), Toast.LENGTH_LONG).show();
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
        captureThread.start();
    }

    private void stopCaptureThread() {
        isCapturing = false;
        if (captureThread != null) {
            try {
                captureThread.join(); // Wait for the thread to finish
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if (handle != null) {
            handle.close();
        }
    }
}
