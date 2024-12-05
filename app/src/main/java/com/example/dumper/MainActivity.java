package com.example.dumper;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.pcap4j.core.PcapDumper;
import org.pcap4j.core.PcapHandle;
import org.pcap4j.core.PcapNetworkInterface;
import org.pcap4j.core.Pcaps;
import org.pcap4j.packet.Packet;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private PcapHandle handle;
    private ExecutorService executor;
    private boolean isCapturing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView statusText = findViewById(R.id.statusText);
        Button captureButton = findViewById(R.id.captureButton);

        executor = Executors.newSingleThreadExecutor();

        captureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isCapturing) {
                    // Start packet capture
                    startCapture(statusText, captureButton);
                } else {
                    // Stop packet capture
                    stopCapture(statusText, captureButton);
                }
            }
        });
    }

    private void startCapture(TextView statusText, Button captureButton) {
        executor.submit(() -> {
            try {
                runOnUiThread(() -> {
                    statusText.setText("Status: Capturing...");
                    captureButton.setText("Stop Capture");
                });

                String interfaceName = "wlan0"; // Replace with desired interface
                PcapNetworkInterface nif = Pcaps.getDevByName(interfaceName);
                if (nif == null) {
                    throw new IllegalArgumentException("No such interface: " + interfaceName);
                }

                File pcapFile = new File("/sdcard/dump.pcap");
                handle = nif.openLive(65536, PcapNetworkInterface.PromiscuousMode.PROMISCUOUS, 10);
                PcapDumper dumperHandle = handle.dumpOpen(pcapFile.getAbsolutePath());


                isCapturing = true;
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
    }

    private void stopCapture(TextView statusText, Button captureButton) {
        isCapturing = false;
        runOnUiThread(() -> {
            statusText.setText("Status: Not Capturing");
            captureButton.setText("Start Capture");
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (handle != null && handle.isOpen()) {
            handle.close();
        }
        executor.shutdownNow();
    }
}
