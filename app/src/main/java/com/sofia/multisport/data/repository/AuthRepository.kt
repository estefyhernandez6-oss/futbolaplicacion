import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.sofia.multisport.data.models.UsuarioApp
import kotlinx.coroutines.tasks.await

class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val usuariosCollection = db.collection("usuarios")

    // Registrar o actualizar los datos del usuario en Firestore
    suspend fun guardarPerfilUsuario(
        nombre: String,
        correo: String,
        esRepresentante: Boolean
    ): Result<Unit> {
        return try {
            val currentUserId = auth.currentUser?.uid ?: throw Exception("No hay usuario autenticado")

            val usuarioApp = UsuarioApp(
                id = currentUserId,
                nombre = nombre,
                correo = correo,
                esRepresentanteEquipo = esRepresentante
            )

            // Guardar o sobrescribir el documento en la colección "usuarios" usando su UID como ID
            usuariosCollection.document(currentUserId).set(usuarioApp).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Obtener la información del usuario actual en tiempo real o por consulta única
    suspend fun obtenerUsuarioActual(): UsuarioApp? {
        val currentUserId = auth.currentUser?.uid ?: return null
        val snapshot = usuariosCollection.document(currentUserId).get().await()
        return snapshot.toObject(UsuarioApp::class.java)
    }
}