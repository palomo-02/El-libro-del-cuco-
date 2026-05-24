package com.example.ellibrodelcuco.data.repository;

import com.example.ellibrodelcuco.data.callback.Callback;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class AuthRepository {

    private static AuthRepository instance;

    private final FirebaseAuth auth;
    private final FirebaseFirestore db;

    private AuthRepository() {
        this.auth = FirebaseAuth.getInstance();
        this.db = FirebaseFirestore.getInstance();
    }

    public static synchronized AuthRepository getInstance() {
        if (instance == null) instance = new AuthRepository();
        return instance;
    }

    public FirebaseUser getCurrentUser() {
        return auth.getCurrentUser();
    }

    public boolean isLoggedIn() {
        return auth.getCurrentUser() != null;
    }

    public void signIn(String email, String password, Callback<FirebaseUser> callback) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = auth.getCurrentUser();
                        asignarAdminSiProcede(user);
                        callback.onSuccess(user);
                    } else {
                        callback.onError(traducirError(task.getException()));
                    }
                });
    }

    public void register(String email, String password, Callback<FirebaseUser> callback) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = auth.getCurrentUser();
                        if (user != null) crearDocumentoUsuario(user, email);
                        callback.onSuccess(user);
                    } else {
                        callback.onError(traducirError(task.getException()));
                    }
                });
    }

    public void updateDisplayName(String newName, Callback<Void> callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            callback.onError("No hay sesión activa");
            return;
        }
        UserProfileChangeRequest request = new UserProfileChangeRequest.Builder()
                .setDisplayName(newName)
                .build();
        user.updateProfile(request)
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void signOut() {
        auth.signOut();
    }

    // crea el doc del usuario en firestore cuando se registra
    private void crearDocumentoUsuario(FirebaseUser user, String email) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("email", email);
        userData.put("rachaActual", 0L);
        userData.put("rachaMaxima", 0L);
        userData.put("metaAnual", 0L);
        userData.put("fotoUrl", "");
        userData.put("rol", "usuario");
        db.collection("usuarios")
                .document(user.getUid())
                .set(userData, SetOptions.merge());
    }

    // si el email es el de admin le ponemos el rol directamente
    private void asignarAdminSiProcede(FirebaseUser user) {
        if (user == null) return;
        if ("admin@ellibrodelcuco.com".equals(user.getEmail())) {
            Map<String, Object> rolData = new HashMap<>();
            rolData.put("rol", "admin");
            db.collection("usuarios")
                    .document(user.getUid())
                    .set(rolData, SetOptions.merge());
        }
    }

    private String traducirError(Exception e) {
        if (e == null) return "Error desconocido";
        String msg = e.getMessage();
        if (msg == null) return "Error desconocido";
        if (msg.contains("no user record") || msg.contains("USER_NOT_FOUND"))
            return "No existe ninguna cuenta con ese email";
        if (msg.contains("password is invalid") || msg.contains("INVALID_LOGIN_CREDENTIALS") || msg.contains("INVALID_PASSWORD"))
            return "Email o contraseña incorrectos";
        if (msg.contains("badly formatted") || msg.contains("INVALID_EMAIL"))
            return "El formato del email no es válido";
        if (msg.contains("network error") || msg.contains("NETWORK_REQUEST_FAILED"))
            return "Sin conexión. Comprueba tu internet";
        if (msg.contains("too many requests") || msg.contains("TOO_MANY_ATTEMPTS"))
            return "Demasiados intentos. Espera un momento";
        if (msg.contains("email address is already in use"))
            return "Este email ya está registrado";
        if (msg.contains("user disabled"))
            return "Esta cuenta ha sido desactivada";
        return "Error de autenticación. Inténtalo de nuevo";
    }
}