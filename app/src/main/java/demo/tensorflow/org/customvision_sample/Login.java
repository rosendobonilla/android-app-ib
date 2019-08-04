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
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class Login extends AppCompatActivity {

    private Button botonLogin;
    private EditText email, pass;
    private TextView linkRegistrar;
    private FirebaseAuth mAtuh;
    private ProgressDialog loadingBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);


        mAtuh =  FirebaseAuth.getInstance();

        linkRegistrar = (TextView) findViewById(R.id.text_Link_Registro);
        email = (EditText) findViewById(R.id.email_Login_Page);
        pass = (EditText) findViewById(R.id.pass_Login_page);
        botonLogin = (Button) findViewById(R.id.boton_Login);
        loadingBar = new ProgressDialog(this);

        linkRegistrar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                abrirRegistroActivity();
            }
        });

        botonLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PermitirAccesoUsuario();
            }
        });
    }

    private void PermitirAccesoUsuario() {
        String mail = email.getText().toString();
        String passwd = pass.getText().toString();

        if(TextUtils.isEmpty(mail)){
            Toast.makeText(this, "Ingresa tu correo electrónico.", Toast.LENGTH_SHORT).show();
        }
        else if(TextUtils.isEmpty(passwd)){
            Toast.makeText(this, "Ingresa tu contraseña.", Toast.LENGTH_SHORT).show();
        }
        else{

            loadingBar.setTitle("Validando credenciales");
            loadingBar.setMessage("Por favor espera mientras se verifican tus credenciales.");
            loadingBar.show();
            loadingBar.setCanceledOnTouchOutside(true);

            mAtuh.signInWithEmailAndPassword(mail,passwd)
                    .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if(task.isSuccessful()){
                                abrirMainActivity();
                                Toast.makeText(Login.this, "Ingreso exitoso.", Toast.LENGTH_SHORT).show();
                                loadingBar.dismiss();
                            }
                            else{
                                String mensaje = task.getException().getMessage();
                                Toast.makeText(Login.this, "Ocurrió un error: " + mensaje, Toast.LENGTH_SHORT).show();
                                loadingBar.dismiss();
                            }
                        }
                    });
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        FirebaseUser currentUser = mAtuh.getCurrentUser();


        if(currentUser != null){
            abrirMainActivity();
        }
    }

    private void abrirRegistroActivity() {
        Intent registroIntent = new Intent(Login.this, RegistroActivity.class);
        startActivity(registroIntent);
    }

    private void abrirMainActivity() {
        Intent setupIntent = new Intent(Login.this, MainActivity.class);
        setupIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(setupIntent);
        finish();
    }
}
