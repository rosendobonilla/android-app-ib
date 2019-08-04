/*
 * Copyright 2016 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package demo.tensorflow.org.customvision_sample;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Typeface;

import android.hardware.Camera;
import android.media.ImageReader.OnImageAvailableListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.Size;
import android.util.TypedValue;
import android.view.Display;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
//import com.google.firebase.firestore.WriteBatch;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.Vector;

import demo.tensorflow.org.customvision_sample.OverlayView.DrawCallback;
import demo.tensorflow.org.customvision_sample.env.BorderedText;
import demo.tensorflow.org.customvision_sample.env.Logger;

public class ClassifierActivity extends CameraActivity implements OnImageAvailableListener {
    private static final Logger LOGGER = new Logger();

    private static final Size DESIRED_PREVIEW_SIZE = new Size(640, 480);
    //private static final Object Locale = "US" ;

    private Integer sensorOrientation;
    private MSCognitiveServicesClassifier classifier;

    private BorderedText borderedText;

    private FirebaseAuth mAuth;

    private static int contador_org, contador_al, contador_papel, contador_vidrio, contador_plas, contador_total;


    String currentUserID, userHandle;

    //FIRESTORE

    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private DocumentReference docRef;

    //Variables Bluetooth
    //-------------------------------------------
    Handler bluetoothIn;
    final int handlerState = 0;
    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocket = null;
    private StringBuilder DataStringIN = new StringBuilder();
    private ConnectedThread ConexionBT;
    // Identificador unico de servicio - SPP UUID
    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    // String para la direccion MAC
    private static String address = null;
    //-------------------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();

        currentUserID = mAuth.getCurrentUser().getUid();
        docRef = db.collection("Users").document(currentUserID);

        /*docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        userHandle = document.getString("userHandle");
                    }
                }
            }
        });*/

        db.collection("Users")
                .whereEqualTo("userId", currentUserID)
                .limit(1)
                .get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        userHandle = document.getString("userHandle");
                    }
                } else {
                    Log.d("User data: USER HANDLE", "Error getting documents: ", task.getException());
                }
            }
        });

        //Bluetooth
        btAdapter = BluetoothAdapter.getDefaultAdapter(); // get Bluetooth adapter
        VerificarEstadoBT();
    }


    //Comienza parte Bluetooth
    @Override
    public void onResume()
    {
        super.onResume();
        //Consigue la direccion MAC desde DeviceListActivity via intent
        Intent intent = getIntent();
        //Consigue la direccion MAC desde DeviceListActivity via EXTRA
        address = intent.getStringExtra(DispositivosBT.EXTRA_DEVICE_ADDRESS);//<-<- PARTE A MODIFICAR >->->
        //Toast.makeText(this, "MAC add: " + address, Toast.LENGTH_SHORT).show();
        //Setea la direccion MAC
        BluetoothDevice device = btAdapter.getRemoteDevice(address);

        try
        {
            btSocket = createBluetoothSocket(device);
        } catch (IOException e) {
            Toast.makeText(getBaseContext(), "La creacción del Socket fallo", Toast.LENGTH_LONG).show();
        }
        // Establece la conexión con el socket Bluetooth.
        try
        {
            btSocket.connect();
        } catch (IOException e) {
            try {
                btSocket.close();
            } catch (IOException e2) {
                Toast.makeText(getBaseContext(), "Error: "+ e2.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
        ConexionBT = new ConnectedThread(btSocket);
        ConexionBT.start();
    }

    @Override
    public void onPause()
    {
        super.onPause();
        try
        { // Cuando se sale de la aplicación esta parte permite
            // que no se deje abierto el socket
            btSocket.close();
        } catch (IOException e2) {}
    }

    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException
    {
        //crea un conexion de salida segura para el dispositivo
        //usando el servicio UUID
        return device.createRfcommSocketToServiceRecord(BTMODULEUUID);
    }


    //Comprueba que el dispositivo Bluetooth Bluetooth está disponible y solicita que se active si está desactivado
    private void VerificarEstadoBT() {

        if(btAdapter==null) {
            Toast.makeText(getBaseContext(), "El dispositivo no soporta bluetooth", Toast.LENGTH_LONG).show();
        } else {
            if (btAdapter.isEnabled()) {
            } else {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, 1);
            }
        }
    }



    private class ConnectedThread extends Thread
    {
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket)
        {
            OutputStream tmpOut = null;
            try
            {
                tmpOut = socket.getOutputStream();
            }
            catch (IOException e) { }

            mmOutStream = tmpOut;
        }

        //Envio de trama
        public void write(String input)
        {
            try {
                mmOutStream.write(input.getBytes());
                Log.d("Bluetooth success: ", "Dato enviado correctamente ");
            }
            catch (IOException e)
            {
                //si no es posible enviar datos se cierra la conexión
                //Toast.makeText(getBaseContext(), "La Conexión fallo", Toast.LENGTH_LONG).show();
                Log.d("Bluetooth: ", "Error enviando datos: " + e.getMessage());
                finish();
            }
        }
    }

    //Termina parte Bluetooth


    @Override
    protected int getLayoutId() {
        return R.layout.camera_connection_fragment;
    }

    @Override
    protected Size getDesiredPreviewFrameSize() {
        return DESIRED_PREVIEW_SIZE;
    }

    private static final float TEXT_SIZE_DIP = 10;

    @Override
    public void onPreviewSizeChosen(final Size size, final int rotation) {
        final float textSizePx = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_DIP, getResources().getDisplayMetrics());
        borderedText = new BorderedText(textSizePx);
        borderedText.setTypeface(Typeface.MONOSPACE);

        classifier = new MSCognitiveServicesClassifier(ClassifierActivity.this);

        previewWidth = size.getWidth();
        previewHeight = size.getHeight();

        final Display display = getWindowManager().getDefaultDisplay();
        final int screenOrientation = display.getRotation();

        LOGGER.i("Sensor orientation: %d, Screen orientation: %d", rotation, screenOrientation);

        sensorOrientation = rotation + screenOrientation;

        LOGGER.i("Initializing at size %dx%d", previewWidth, previewHeight);
        rgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Config.ARGB_8888);

        yuvBytes = new byte[3][];

        addCallback(
                new DrawCallback() {
                    @Override
                    public void drawCallback(final Canvas canvas) {
                        renderDebug(canvas);
                    }
                });
    }

    //Recibo objeto Camera para poder para el Preview

    protected void processImageRGBbytes(int[] rgbBytes, final Camera camera) {
        rgbFrameBitmap.setPixels(rgbBytes, 0, previewWidth, 0, 0, previewWidth, previewHeight);

        runInBackground(
                new Runnable() {
                    @Override
                    public void run() {
                        final long startTime = SystemClock.uptimeMillis();
                        Classifier.Recognition r = classifier.classifyImage(rgbFrameBitmap, sensorOrientation);
                        lastProcessingTimeMs = SystemClock.uptimeMillis() - startTime;

                        final List<Classifier.Recognition> results = new ArrayList<>();

                        if (r.getConfidence() > 0.7) {
                            results.add(r);
                        }
                        if (results != null && results.size() > 0 && !results.get(0).getTitle().equals("Otro")){
                            LOGGER.i("Detect: %s", results);
                            camera.stopPreview();
                            //Paso el Camera como parámetro para reiniciar el Preview desde el método y el resultado
                            guardarEnDB(camera, results.get(0).getTitle());
                        }
                        if (resultsView == null) {
                            resultsView = findViewById(R.id.results);
                        }

                        resultsView.setResults(results);

                        requestRender();
                        computing = false;
                        if (postInferenceCallback != null) {
                            postInferenceCallback.run();
                        }
                    }
                });

    }

    //Recibo Camera para detener el Preview porque desde el run hace tareas asíncronas
    private void guardarEnDB(final Camera camera, final String res) {
        //Toast.makeText(this, "Objeto detectado", Toast.LENGTH_SHORT).show();


        //Formatear fecha a ISOString 8061
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US);
        final String ISODate = sdf.format(new Date());

        db.collection("Stats")
                .document(userHandle)
                .get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        contador_org = document.getLong("organica").intValue();
                        contador_al = document.getLong("aluminio").intValue();
                        contador_papel = document.getLong("papel").intValue();
                        contador_vidrio = document.getLong("vidrio").intValue();
                        contador_plas = document.getLong("plastico").intValue();
                        contador_total = document.getLong("total").intValue();
                    } else {
                        Log.d("GET DOC TRASH STATS", "No such document");
                    }
                } else {
                    Log.d("GET DOC TRASH STATS: " , "get failed with ", task.getException());
                }
            }
        });

        new CountDownTimer(2500, 1000) {

            public void onTick(long millisUntilFinished) {
                LOGGER.i("Tiempo restante: %s", millisUntilFinished / 1000);
            }

            public void onFinish() {

                if(res.equals("Organica")) {


                    /*runInBackground(
                            new Runnable() {
                                @Override
                                public void run() {
                                    MyConexionBT.write("0");
                                    while(true){
                                        Log.d("RUNNABLE: " , " ejecutandose ");
                                    }
                                }
                            });*/

                    //Test enviar caracter Bluetooth
                    //Es necesario que el envio de datos por BT se abra en un nuevo hilo en background
                    //sino interfiere con el uso de la camara
                    new Bluetooth2Plano().execute("4");


                    //TODO Cambiar este metodo para usar Retrofit y Cloud Functions
                    //Registra un nuevo documento cada vez que reconoce un material

                    HashMap trashData = new HashMap();

                    trashData.put("type","organica");
                    trashData.put("userHandle", userHandle);
                    trashData.put("createdAt", ISODate);

                    /*db.collection("Trash")
                            .document(userHandle)
                            .set(data)
                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    Log.d("New doc trash", "DocumentSnapshot written");
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {

                                }
                            });*/

                    // Get a new write batch
                    //WriteBatch batch = db.batch();

                    db.collection("Trash")
                            .add(trashData)
                            .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                @Override
                                public void onSuccess(DocumentReference documentReference) {
                                    Log.d("ADD TRASH: ", "DocumentSnapshot successfully written!");

                                    db.collection("Stats").document(userHandle)
                                            .update(
                                                    "organica", contador_org+1,
                                                    "total", contador_total+1
                                            )
                                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                @Override
                                                public void onSuccess(Void aVoid) {
                                                    Log.d("ACTUALIZAR ORGANIC", "DocumentSnapshot successfully updated!");
                                                }
                                            })
                                            .addOnFailureListener(new OnFailureListener() {
                                                @Override
                                                public void onFailure(@NonNull Exception e) {
                                                    Log.w("ACTUALIZAR ORGANIC", "Error updating document", e);
                                                }
                                            });
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.w("ADD TRASH: ", "Error writing document", e);
                        }
                    });
                    /* Guarda datos de basura
                    DocumentReference trash = db.collection("Trash").document(userHandle);
                    batch.set(trash, trashData);

                    // Guardar datos en Estadisticas
                    contador_org = contador_org + 1;
                    DocumentReference stats = db.collection("Stats").document(userHandle);
                    batch.update(stats, "organic", contador_org);

                    // Delete the city 'LA'
                    DocumentReference laRef = db.collection("cities").document("LA");
                    batch.delete(laRef);

                    // Commit the batch
                    batch.commit().addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            Log.d("BATCH WRITE", "Tareas ejecutadas");
                        }
                    });*/



                            /*.addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                @Override
                                public void onSuccess(DocumentReference documentReference) {
                                    Log.d("New doc trash", "DocumentSnapshot written with ID: " + documentReference.getId());
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Log.w("New doc trash", "Error adding document", e);
                                }
                            });*/

                    //2019-06-10T18:20:26-0500
                    //2019-05-19T04:51:40.842Z
                    //Este método servira en el backend para establecer los contadores
                    /*db.collection("Trash")
                            .whereEqualTo("userHandle", userHandle)
                            .whereEqualTo("type", "organic")
                            .get()
                            .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                @Override
                                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                    if (task.isSuccessful()) {

                                        for (QueryDocumentSnapshot document : task.getResult()) {
                                            Log.d("Doc", document.getId() + " => " + document.getData());
                                            //contador_org =  document.getLong("organic")
                                        }
                                    } else {
                                        Log.d("Doc", "Error getting documents: ", task.getException());
                                    }
                                }
                            });*/

                    /*docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                            if (task.isSuccessful()) {
                                DocumentSnapshot document = task.getResult();
                                if (document.exists()) {
                                    contador_org = document.getLong("organica").intValue();
                                    //contador_org = Integer.parseInt(valorTipoBasuraString);
                                    //Toast.makeText(ClassifierActivity.this, "No. " + valorTipoBasuraString, Toast.LENGTH_LONG).show();
                                    contador_org = contador_org + 1;

                                    docRef.update("organica", contador_org)
                                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                @Override
                                                public void onSuccess(Void aVoid) {
                                                    LOGGER.i("Actualizacion de contador satisfactoria.");
                                                }
                                            })
                                            .addOnFailureListener(new OnFailureListener() {
                                                @Override
                                                public void onFailure(@NonNull Exception e) {
                                                    LOGGER.i("Hubo un error: " + e.toString());
                                                }
                                            });

                                }
                            }
                        }
                    });*/

                }
                else if(res.equals("Vidrio")) {

                    new Bluetooth2Plano().execute("3");

                    HashMap trashData = new HashMap();

                    trashData.put("type","vidrio");
                    trashData.put("userHandle", userHandle);
                    trashData.put("createdAt", ISODate);

                    db.collection("Trash")
                            .add(trashData)
                            .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                @Override
                                public void onSuccess(DocumentReference documentReference) {
                                    Log.d("ADD TRASH: ", "DocumentSnapshot successfully written!");

                                    db.collection("Stats").document(userHandle)
                                            .update(
                                                    "vidrio", contador_vidrio+1,
                                                    "total", contador_total+1
                                            )
                                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                @Override
                                                public void onSuccess(Void aVoid) {
                                                    Log.d("ACTUALIZAR VIDRIO", "DocumentSnapshot successfully updated!");
                                                }
                                            })
                                            .addOnFailureListener(new OnFailureListener() {
                                                @Override
                                                public void onFailure(@NonNull Exception e) {
                                                    Log.w("ACTUALIZAR VIDRIO", "Error updating document", e);
                                                }
                                            });
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.w("ADD TRASH: ", "Error writing document", e);
                        }
                    });

                }
                else if(res.equals("Papel")) {

                    new Bluetooth2Plano().execute("2");

                    HashMap trashData = new HashMap();

                    trashData.put("type","papel");
                    trashData.put("userHandle", userHandle);
                    trashData.put("createdAt", ISODate);

                    db.collection("Trash")
                            .add(trashData)
                            .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                @Override
                                public void onSuccess(DocumentReference documentReference) {
                                    Log.d("ADD TRASH: ", "DocumentSnapshot successfully written!");

                                    db.collection("Stats").document(userHandle)
                                            .update(
                                                    "papel", contador_papel+1,
                                                    "total", contador_total+1
                                            )
                                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                @Override
                                                public void onSuccess(Void aVoid) {
                                                    Log.d("ACTUALIZAR PAPEL", "DocumentSnapshot successfully updated!");
                                                }
                                            })
                                            .addOnFailureListener(new OnFailureListener() {
                                                @Override
                                                public void onFailure(@NonNull Exception e) {
                                                    Log.w("ACTUALIZAR PAPEL", "Error updating document", e);
                                                }
                                            });
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.w("ADD TRASH: ", "Error writing document", e);
                        }
                    });

                }
                else if(res.equals("Aluminio")) {

                    new Bluetooth2Plano().execute("1");

                    HashMap trashData = new HashMap();

                    trashData.put("type","aluminio");
                    trashData.put("userHandle", userHandle);
                    trashData.put("createdAt", ISODate);

                    db.collection("Trash")
                            .add(trashData)
                            .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                @Override
                                public void onSuccess(DocumentReference documentReference) {
                                    Log.d("ADD TRASH: ", "DocumentSnapshot successfully written!");

                                    db.collection("Stats").document(userHandle)
                                            .update(
                                                    "aluminio", contador_al+1,
                                                    "total", contador_total+1
                                            )
                                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                @Override
                                                public void onSuccess(Void aVoid) {
                                                    Log.d("ACTUALIZAR ALUMINIO", "DocumentSnapshot successfully updated!");
                                                }
                                            })
                                            .addOnFailureListener(new OnFailureListener() {
                                                @Override
                                                public void onFailure(@NonNull Exception e) {
                                                    Log.w("ACTUALIZAR ALUMINIO", "Error updating document", e);
                                                }
                                            });
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.w("ADD TRASH: ", "Error writing document", e);
                        }
                    });

                }
                else if(res.equals("Plastico")) {

                    new Bluetooth2Plano().execute("0");

                    HashMap trashData = new HashMap();

                    trashData.put("type","plastico");
                    trashData.put("userHandle", userHandle);
                    trashData.put("createdAt", ISODate);

                    db.collection("Trash")
                            .add(trashData)
                            .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                @Override
                                public void onSuccess(DocumentReference documentReference) {
                                    Log.d("ADD TRASH: ", "DocumentSnapshot successfully written!");

                                    db.collection("Stats").document(userHandle)
                                            .update(
                                                    "plastico", contador_plas+1,
                                                    "total", contador_total+1
                                            )
                                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                @Override
                                                public void onSuccess(Void aVoid) {
                                                    Log.d("ACTUALIZAR PLASTICO", "DocumentSnapshot successfully updated!");
                                                }
                                            })
                                            .addOnFailureListener(new OnFailureListener() {
                                                @Override
                                                public void onFailure(@NonNull Exception e) {
                                                    Log.w("ACTUALIZAR PLASTICO", "Error updating document", e);
                                                }
                                            });
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.w("ADD TRASH: ", "Error writing document", e);
                        }
                    });

                }
                //camera.release();
                camera.startPreview();
            }

        }.start();
    }


    //Clase que hereda de AsyncTask para enviar datos por bluetooth en segundo plano
    public class Bluetooth2Plano extends AsyncTask<String, Integer, String> {

        @Override
        protected String doInBackground(String... params) {
            try {
                ConexionBT.write(params[0]);
            }
            catch (Exception e) { e.printStackTrace(); }

            return "OK " + params[0];
        }

        @Override
        protected void onPostExecute(String result) {
            Log.w("Final AsyncTask", "Result: " + result);
        }
    }


    @Override
    public void onSetDebug(boolean debug) {
    }

    private void renderDebug(final Canvas canvas) {
        if (!isDebug()) {
            return;
        }

        final Vector<String> lines = new Vector<String>();
        lines.add("Inference time: " + lastProcessingTimeMs + "ms");
        borderedText.drawLines(canvas, 10, canvas.getHeight() - 10, lines);
    }
}
