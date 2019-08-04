package demo.tensorflow.org.customvision_sample;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class RegistroActivity extends AppCompatActivity {

    private EditText email, pass, passConf;
    private Button botonRegistrar;
    private FirebaseAuth mAuth;
    private ProgressDialog loadingBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registro);

        mAuth = FirebaseAuth.getInstance();

        email = (EditText) findViewById(R.id.email_Register_Page);
        pass  = (EditText) findViewById(R.id.pass_Register_page);
        passConf = (EditText) findViewById(R.id.pass_confirm_Register_page);
        botonRegistrar = (Button) findViewById(R.id.boton_Registrarse);
        loadingBar = new ProgressDialog(this);

        botonRegistrar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CrearCuentaNueva();
            }
        });
    }

    private void CrearCuentaNueva() {
        String mail = email.getText().toString();
        String passw = pass.getText().toString();
        String passconfirm = passConf.getText().toString();

        if(TextUtils.isEmpty(mail)){
            Toast.makeText(this, "Por favor, ingresa tu email.", Toast.LENGTH_SHORT).show();
        }
        else if(TextUtils.isEmpty(passw)){
            Toast.makeText(this, "Por favor, ingresa una contraseña.", Toast.LENGTH_SHORT).show();
        }
        else if(TextUtils.isEmpty(passconfirm)){
            Toast.makeText(this, "Por favor, confirma tu contraseña.", Toast.LENGTH_SHORT).show();
        }
        else if(!passw.equals(passconfirm)){
            Toast.makeText(this, "Las contraseñas no coinciden.", Toast.LENGTH_SHORT).show();
        }
        else{

            loadingBar.setTitle("Creando cuenta");
            loadingBar.setMessage("Por favor espera mientras se procesa la solicitud.");
            loadingBar.show();
            loadingBar.setCanceledOnTouchOutside(true);

            mAuth.createUserWithEmailAndPassword(mail,passw)
                    .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if(task.isSuccessful()){
                                abrirSetupActivity();
                                Toast.makeText(RegistroActivity.this, "La cuenta ha sido creada exitosamente.", Toast.LENGTH_SHORT).show();
                                loadingBar.dismiss();
                            }
                            else{
                                String mensaje = task.getException().getMessage();
                                Toast.makeText(RegistroActivity.this, "Ocurrió un error: " + mensaje, Toast.LENGTH_SHORT).show();
                                loadingBar.dismiss();
                            }
                        }
                    });
        }

    }

    @Override
    protected void onStart() {
        super.onStart();

        FirebaseUser currentUser = mAuth.getCurrentUser();


        if(currentUser != null){
            abrirMainActivity();
        }
    }

    private void abrirSetupActivity() {
        Intent setupIntent = new Intent(RegistroActivity.this, SetupActivity.class);
        setupIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(setupIntent);
        finish();
    }

    private void abrirMainActivity() {
        Intent setupIntent = new Intent(RegistroActivity.this, MainActivity.class);
        setupIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(setupIntent);
        finish();
    }
}
