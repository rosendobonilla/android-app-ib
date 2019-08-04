package demo.tensorflow.org.customvision_sample;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;
import com.yalantis.ucrop.UCrop;

import java.io.File;
import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;

public class SetupActivity extends AppCompatActivity {

    private EditText usuario, nombre, lugar_cesto;
    private Button botonGuardar;
    private CircleImageView fotoPerfil;

    private FirebaseAuth mAuth;
    //private DatabaseReference UsersRef, RefAddTipos;
    private StorageReference ImagenPerfilRef;

    private ProgressDialog loadingBar;

    String currentUserID;
    final static int Galeria_Pick = 1;
    private final String SAMPLE_CROPPED_IMG_NAME = "SampleCroppImg";

    String imagenPerfilRef;
    Boolean fotoElegida = false;

    //FIRESTORE

    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private DocumentReference docRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);

        mAuth = FirebaseAuth.getInstance();
        currentUserID = mAuth.getCurrentUser().getUid();
        //UsersRef = FirebaseDatabase.getInstance().getReference().child("Users").child(currentUserID);
        //RefAddTipos = FirebaseDatabase.getInstance().getReference().child("Users").child(currentUserID).child("tipos_basura");

        docRef = db.collection("Users").document(currentUserID);


        ImagenPerfilRef = FirebaseStorage.getInstance().getReference().child("imagenes_perfil");

        usuario = (EditText) findViewById(R.id.text_username);
        nombre = (EditText) findViewById(R.id.text_nombre_completo);
        lugar_cesto = (EditText) findViewById(R.id.text_lugar_cesto);
        botonGuardar = (Button) findViewById(R.id.boton_guardar_usuario);
        fotoPerfil = (CircleImageView) findViewById(R.id.foto_perfil);
        loadingBar = new ProgressDialog(this);


        botonGuardar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GuardarInfoUsuario();
            }
        });

        fotoPerfil.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(new Intent().setAction(Intent.ACTION_GET_CONTENT).setType("image/*"),Galeria_Pick);

            }
        });

        /*UsersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    if(dataSnapshot.hasChild("profileImage")){
                        String image = dataSnapshot.child("profileImage").getValue().toString();
                        Picasso.with(getApplicationContext()).load(image).placeholder(R.drawable.default_profile).into(fotoPerfil);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });*/

        docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (!document.exists()) {
                        String imagen  = document.getString("profileImage");
                        Toast.makeText(SetupActivity.this, "Result: " + imagen, Toast.LENGTH_SHORT).show();
                        Picasso.with(getApplicationContext()).load(imagen).placeholder(R.drawable.default_profile).into(fotoPerfil);
                    }
                }
            }
        });


    }

    /*public void checarVersionAndroid(){

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, 555);
            }catch (Exception e){

            }
        } else {
            pickImage();
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == 555 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            pickImage();
        } else {
            checarVersionAndroid();
        }
    }*/


    //


    private void startCrop(@NonNull Uri imageUri) {
        String destinationFileName = SAMPLE_CROPPED_IMG_NAME;
        destinationFileName += ".jpg";
        UCrop uCrop = UCrop.of(imageUri, Uri.fromFile(new File(getCacheDir(), destinationFileName)));

        uCrop.withAspectRatio(1,1);

        uCrop.withMaxResultSize(200,200);
        uCrop.withOptions(getCropOptions());

        uCrop.start(SetupActivity.this);

    }

    private UCrop.Options getCropOptions(){
        UCrop.Options options = new UCrop.Options();

        options.setCompressionQuality(70);

        options.setHideBottomControls(false);
        options.setFreeStyleCropEnabled(true);

        options.setStatusBarColor(getResources().getColor(R.color.colorPrimaryDark));
        options.setToolbarColor(getResources().getColor(R.color.colorPrimary));

        options.setToolbarTitle("Recortar imagen");

        return options;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == Galeria_Pick && resultCode == RESULT_OK){
            Uri imageUri = data.getData();
            if(imageUri != null){
                startCrop(imageUri);
            }
        }else if(requestCode == UCrop.REQUEST_CROP && resultCode == RESULT_OK){
            Uri imageUriResultCrop = UCrop.getOutput(data);
            if(imageUriResultCrop != null) {
                //fotoPerfil.setImageURI(imageUriResultCrop);

                try {
                    fotoElegida = true;
                    loadingBar.setTitle("Imagen de perfil");
                    loadingBar.setMessage("Por favor espera mientras se actualiza tu imagen de perfil.");
                    loadingBar.show();
                    loadingBar.setCanceledOnTouchOutside(true);

                    //Uri resultUri = imageUriResultCrop.getUri();
                    final StorageReference rutaImagen = ImagenPerfilRef.child(currentUserID + ".jpg");
                    imagenPerfilRef = currentUserID + ".jpg";
                    UploadTask uploadTask = rutaImagen.putFile(imageUriResultCrop);


                    Task<Uri> urlTask = uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                        @Override
                        public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                            if (!task.isSuccessful()) {
                                throw task.getException();
                            }

                            // Continue with the task to get the download URL
                            return rutaImagen.getDownloadUrl();
                        }
                    }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                        @Override
                        public void onComplete(@NonNull Task<Uri> task) {
                            if (task.isSuccessful()) {
                                String photoStringLink = "";
                                Uri downloadUri = task.getResult();
                                if (downloadUri != null) {

                                    photoStringLink = downloadUri.toString(); //YOU WILL GET THE DOWNLOAD URL HERE !!!!
                                }

                                HashMap imagenperfil = new HashMap();

                                imagenperfil.put("profileImage", photoStringLink);

                                /*UsersRef.child("profileImage").setValue(photoStringLink)
                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {
                                                if (task.isSuccessful()) {

                                                    //Intent selfintent = new Intent(SetupActivity.this, SetupActivity.class);
                                                    //startActivity(selfintent);
                                                    Toast.makeText(SetupActivity.this, "Foto de perfil establecida correctamente.", Toast.LENGTH_SHORT).show();
                                                    loadingBar.dismiss();
                                                } else {
                                                    String mensage = task.getException().toString();
                                                    Toast.makeText(SetupActivity.this, "Ocurrió un error: " + mensage, Toast.LENGTH_SHORT).show();
                                                    loadingBar.dismiss();
                                                }
                                            }
                                        });*/

                                db.collection("Users").document(currentUserID)
                                        .set(imagenperfil)
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                Toast.makeText(SetupActivity.this, "Foto de perfil establecida correctamente.", Toast.LENGTH_SHORT).show();
                                                loadingBar.dismiss();
                                            }
                                        })
                                        .addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                String mensage = e.toString();
                                                Toast.makeText(SetupActivity.this, "Ocurrió un error: " + mensage, Toast.LENGTH_SHORT).show();
                                                loadingBar.dismiss();
                                            }
                                        });


                            } else {
                                Toast.makeText(SetupActivity.this, "error", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });

                }catch (Exception e){

                }
            }
        }
    }


    private void GuardarInfoUsuario() {
        String us = usuario.getText().toString();
        String nom = nombre.getText().toString();
        String lugar = lugar_cesto.getText().toString();

        if(fotoElegida == false){
            Toast.makeText(this, "Elija una foto de perfil.", Toast.LENGTH_SHORT).show();
        }
        else if(TextUtils.isEmpty(us)){
            Toast.makeText(this, "Ingrese un nombre de usuario.", Toast.LENGTH_SHORT).show();
        }
        else if(TextUtils.isEmpty((nom))){
            Toast.makeText(this, "Ingrese tu nombre completo.", Toast.LENGTH_SHORT).show();
        }
        else if(TextUtils.isEmpty(lugar)){
            Toast.makeText(this, "Ingrese la ubicación del cesto de basura.", Toast.LENGTH_SHORT).show();
        }
        else{

            loadingBar.setTitle("Guardar datos");
            loadingBar.setMessage("Por favor espera mientras se procesa la información.");
            loadingBar.show();
            loadingBar.setCanceledOnTouchOutside(true);

            HashMap datos = new HashMap();

            datos.put("usuario", us);
            datos.put("nombre", nom);
            datos.put("lugar_cesto", lugar);
            datos.put("status", "Hey, estoy usando Cesto Inteligente.");
            datos.put("imagenPerfilRef", imagenPerfilRef);
            datos.put("organica", 0);
            datos.put("papel", 0);
            datos.put("aluminio", 0);
            datos.put("vidrio", 0);
            datos.put("plastico", 0);

            /*UsersRef.updateChildren(datos).addOnCompleteListener(new OnCompleteListener() {
                @Override
                public void onComplete(@NonNull Task task) {
                    if(task.isSuccessful()){
                        HashMap tipos = new HashMap();
                        tipos.put("organica",0);
                        tipos.put("aluminio",0);
                        tipos.put("vidrio",0);
                        tipos.put("papel",0);
                        tipos.put("plastico",0);

                        RefAddTipos.updateChildren(tipos).addOnCompleteListener(new OnCompleteListener() {
                            @Override
                            public void onComplete(@NonNull Task task) {

                                if(task.isSuccessful()){
                                    abrirMainActivity();
                                    Toast.makeText(SetupActivity.this, "Datos almacenados correctamente.", Toast.LENGTH_SHORT).show();
                                    loadingBar.dismiss();
                                }
                                else{
                                    String mensage = task.getException().getMessage();
                                    Toast.makeText(SetupActivity.this, "Ocurrió un error: " + mensage, Toast.LENGTH_SHORT).show();
                                    loadingBar.dismiss();
                                }

                            }
                        });

                    }
                    else{
                        String mensage = task.getException().getMessage();
                        Toast.makeText(SetupActivity.this, "Ocurrió un error: " + mensage, Toast.LENGTH_SHORT).show();
                        loadingBar.dismiss();
                    }
                }
            });*/

            db.collection("Users").document(currentUserID).set(datos, SetOptions.merge())
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            abrirMainActivity();
                            Toast.makeText(SetupActivity.this, "Datos almacenados correctamente.", Toast.LENGTH_SHORT).show();
                            loadingBar.dismiss();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            String mensage = e.toString();
                            Toast.makeText(SetupActivity.this, "Ocurrió un error: " + mensage, Toast.LENGTH_SHORT).show();
                            loadingBar.dismiss();
                        }
                    });

        }

    }

    private void abrirMainActivity() {
        Intent setupIntent = new Intent(SetupActivity.this, MainActivity.class);
        setupIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(setupIntent);
        finish();
    }

}


