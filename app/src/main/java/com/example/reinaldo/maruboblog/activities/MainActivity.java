package com.example.reinaldo.maruboblog.activities;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.example.reinaldo.maruboblog.Manifest;
import com.example.reinaldo.maruboblog.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

public class MainActivity extends AppCompatActivity {
    ImageView ImageUserFoto;
    static int PReqCode = 1;
    static int REQUESCODE = 1;
    Uri pickedImgUri;

    private EditText emailUsuario, senhaUsuario,confirmSenha,nomeUsuario;
    private ProgressBar loadingProgress;
    private Button botao;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ImageUserFoto = findViewById(R.id.registroUserFoto);
        emailUsuario = findViewById(R.id.regMail);
        senhaUsuario = findViewById(R.id.regPassword);
        confirmSenha = findViewById(R.id.regPassword2);
        nomeUsuario = findViewById(R.id.regName);
        botao = findViewById(R.id.regBtn);
        loadingProgress = findViewById(R.id.regProgressBar);
        loadingProgress.setVisibility(View.INVISIBLE);

        mAuth = FirebaseAuth.getInstance();

        //Clique do botão registrar
        botao.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                botao.setVisibility(View.INVISIBLE);
                loadingProgress.setVisibility(View.VISIBLE);
                final String email = emailUsuario.getText().toString();
                final String senha = senhaUsuario.getText().toString();
                final String senha2 = confirmSenha.getText().toString();
                final String nome = nomeUsuario.getText().toString();

                if(email.isEmpty() || nome.isEmpty() || senha.isEmpty() || !senha.equals(senha2) ){

                    //se algo der errado: todos os campos devem ser preenchidos
                    mostrarMensagem("Por Favor Verificar todos os campos");
                    botao.setVisibility(View.VISIBLE);
                    loadingProgress.setVisibility(View.INVISIBLE);

                }else{
                    //está tudo bem e todos os campos estão preenchidos agora podemos começar a criar uma conta de usuário
                    //criarContaUsuario() ,cria conta de usuário se o email for válido
                    criarContaUsuario(email, nome, senha);


                }
            }
        });




        ImageUserFoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(Build.VERSION.SDK_INT >= 22){

                    checarRequesicaoParaPermissao();

                }else{
                    abirGaleria();
                }

            }
        });

    }

    //Cria conta do usuario
    private void criarContaUsuario(String email, final String nome, String senha) {


        //ESTE MÉTODO CRIAR ACESSO DE USUÁRIO COM E-MAIL E SENHA ESPECÍFICOS
        mAuth.createUserWithEmailAndPassword(email, senha)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {


                            //Se a tarefa for concluida com sucesso
                            mostrarMensagem("Conta criada com sucesso!");
                            //depois que criamos a conta de usuário, precisamos atualizar esta foto de perfil e nome
                            atualizacaoUsuario(nome, pickedImgUri, mAuth.getCurrentUser());


                        } else {
                            mostrarMensagem("A conta não pôde ser criada! "+ task.getException().getMessage());
                            botao.setVisibility(View.VISIBLE);
                            loadingProgress.setVisibility(View.INVISIBLE);

                        }

                    }
                });
    }

    //atualiza a foto e o nome do ususario
    private void atualizacaoUsuario(final String nome, Uri pickedImgUri, final FirebaseUser atualUsuario) {

        //Primeiro, precisamos fazer o upload da foto do usuário para o Firebase storage e obter o URL
        StorageReference mStorage = FirebaseStorage.getInstance().getReference().child("foto_usuario");

        final StorageReference imageFilePath = mStorage.child(pickedImgUri.getLastPathSegment());

        imageFilePath.putFile(pickedImgUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {

            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                //upload de imagem com sucesso
                //agora podemos obter nosso URL de imagem

                imageFilePath.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        //uri contém o URL da imagem do usuário

                        UserProfileChangeRequest atualizaPerfil = new UserProfileChangeRequest.Builder()
                                .setDisplayName(nome)
                                .setPhotoUri(uri)
                                .build();

                        atualUsuario.updateProfile(atualizaPerfil).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if(task.isSuccessful()){

                                    //atualização de informações do usuário com sucesso
                                    mostrarMensagem("Registro completo!");
                                    atualizaInterfaceUsuario();

                                }
                            }
                        });

                    }
                });


            }
        });

    }

    //chama a tela home
    private void atualizaInterfaceUsuario() {
        Intent homeActivity = new Intent(getApplicationContext(), HomeActivity.class);
        startActivity(homeActivity);
        finish();

    }

    private void mostrarMensagem(String mensagem) {
        Toast.makeText(getApplicationContext(),mensagem,Toast.LENGTH_SHORT).show();

    }


    private void abirGaleria() {


        Intent galeria = new Intent(Intent.ACTION_GET_CONTENT);
        galeria.setType("image/*");
        startActivityForResult(galeria, REQUESCODE);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(resultCode == RESULT_OK && requestCode == REQUESCODE && data != null);

        //o usuário escolheu com sucesso uma imagem que precisamos salvar suas referências a uma variável Uri
        pickedImgUri = data.getData();
        ImageUserFoto.setImageURI(pickedImgUri);


    }



    private void checarRequesicaoParaPermissao() {

        if(ContextCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED){
            if(ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, android.Manifest.permission.READ_EXTERNAL_STORAGE)){

                Toast.makeText(MainActivity.this," Por favor aceite para permissão requerida ", Toast.LENGTH_SHORT).show();
            }else{
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE},
                        PReqCode);
            }

        }
        else{

            abirGaleria();
        }



    }
}