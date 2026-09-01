package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.entity.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        UserEntity::class,
        IncidentEntity::class,
        ProcedureStepEntity::class,
        TrainingEntity::class,
        EventEntity::class,
        NotificationEntity::class,
        SurveyEntity::class,
        SuccessStoryEntity::class,
        SafetyAuditEntity::class,
        ContentEntity::class,
        DocumentAttestationEntity::class,
        HseIaSessionEntity::class,
        HseIaMessageEntity::class
    ],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun hseDao(): HseDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("DELETE FROM contents WHERE id LIKE 'cnt_pdf_%' OR id LIKE 'cnt_img_%' OR id LIKE 'cnt_vid_%' OR id LIKE 'cnt_link_%'")
                database.execSQL("DELETE FROM success_stories WHERE id LIKE 'story_100_days' OR id LIKE 'story_video_%' OR id LIKE 'story_sst_%' OR id LIKE 'story_co2_%' OR id LIKE 'story_temoignage_%' OR id LIKE 'story_epi_%'")
                database.execSQL("DELETE FROM document_attestations WHERE certificateId LIKE 'CERT-SEC-%'")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "cimencam_hse_db_v4"
                ).addMigrations(MIGRATION_4_5).fallbackToDestructiveMigration().addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        CoroutineScope(Dispatchers.IO).launch {
                            getInstance(context).seedInitialData()
                        }
                    }
                }).build()
                INSTANCE = instance
                instance
            }
        }
    }

    suspend fun seedInitialData() {
        val dao = hseDao()
        // Incidents and procedures data seeding

        // Seed default incidents matching screenshot
        dao.insertIncident(
            IncidentEntity(
                title = "Fuite d'huile signalée",
                category = "Environnement",
                severity = "Majeur",
                site = "Site Figuil",
                location = "Atelier de maintenance • Site Figuil",
                description = "Fuite d'huile hydraulique détectée sous le broyeur n°2. Risque de glissade et de pollution du sol. Zone balisée en attente d'intervention de l'équipe de nettoyage spécialisée.",
                reportedBy = "Agent HSE",
                timestampText = "Il y a 2h",
                timestampMillis = System.currentTimeMillis() - 2 * 3600 * 1000,
                status = "En cours"
            )
        )
        dao.insertIncident(
            IncidentEntity(
                title = "Obstruction issue de secours",
                category = "Sécurité",
                severity = "Mineur",
                site = "Site Bonabéri",
                location = "Hangar de stockage Sacs",
                description = "Palettes de ciment entreposées devant la porte d'évacuation Nord. Déplacement immédiat demandé.",
                reportedBy = "Jean-Pierre MVONDO",
                timestampText = "Hier",
                timestampMillis = System.currentTimeMillis() - 24 * 3600 * 1000,
                status = "En cours"
            )
        )
        dao.insertIncident(
            IncidentEntity(
                title = "Câble électrique dénudé",
                category = "Sécurité",
                severity = "Critique",
                site = "Site Nomayos",
                location = "Poste de commande Ensachage",
                description = "Gaine isolante endommagée sur l'alimentation du convoyeur. Consignation effectuée.",
                reportedBy = "Emmanuel BIYA",
                timestampText = "Il y a 2 jours",
                timestampMillis = System.currentTimeMillis() - 48 * 3600 * 1000,
                status = "En cours"
            )
        )

        // Seed 6 steps for each procedure (Fiches Réflexes)
        val accidentSteps = listOf(
            ProcedureStepEntity(procedureId = "accident_corporel", stepNumber = 1, title = "Sécuriser la zone — ne pas déplacer la victime"),
            ProcedureStepEntity(procedureId = "accident_corporel", stepNumber = 2, title = "Appeler les secours : Infirmerie interne ext. 100"),
            ProcedureStepEntity(procedureId = "accident_corporel", stepNumber = 3, title = "Rester auprès de la victime et la rassurer"),
            ProcedureStepEntity(procedureId = "accident_corporel", stepNumber = 4, title = "Identifier les Sauveteurs Secouristes du Travail (SST)"),
            ProcedureStepEntity(procedureId = "accident_corporel", stepNumber = 5, title = "Guider les équipes d'intervention sur le lieu"),
            ProcedureStepEntity(procedureId = "accident_corporel", stepNumber = 6, title = "Remplir la fiche de premier constat d'accident")
        )
        val chemSteps = listOf(
            ProcedureStepEntity(procedureId = "risques_chimiques", stepNumber = 1, title = "Éloigner le personnel non protégé du foyer"),
            ProcedureStepEntity(procedureId = "risques_chimiques", stepNumber = 2, title = "Consulter la Fiche de Données de Sécurité (FDS)"),
            ProcedureStepEntity(procedureId = "risques_chimiques", stepNumber = 3, title = "Enfiler les EPI spécifiques (masque ABEK, gants nitrile)"),
            ProcedureStepEntity(procedureId = "risques_chimiques", stepNumber = 4, title = "Utiliser les kits absorbants et neutralisants"),
            ProcedureStepEntity(procedureId = "risques_chimiques", stepNumber = 5, title = "Aérer la zone et ventiler les locaux"),
            ProcedureStepEntity(procedureId = "risques_chimiques", stepNumber = 6, title = "Évacuer les déchets souillés selon la filière agréée")
        )
        val elecSteps = listOf(
            ProcedureStepEntity(procedureId = "risques_electriques", stepNumber = 1, title = "Couper immédiatement l'alimentation au disjoncteur général"),
            ProcedureStepEntity(procedureId = "risques_electriques", stepNumber = 2, title = "Poser le cadenas de consignation (LOTO)"),
            ProcedureStepEntity(procedureId = "risques_electriques", stepNumber = 3, title = "Vérifier l'absence de tension (VAT) avec appareil certifié"),
            ProcedureStepEntity(procedureId = "risques_electriques", stepNumber = 4, title = "Balisage de sécurité et signalisation d'interdiction"),
            ProcedureStepEntity(procedureId = "risques_electriques", stepNumber = 5, title = "Ne jamais toucher une victime électrisée sans isolant"),
            ProcedureStepEntity(procedureId = "risques_electriques", stepNumber = 6, title = "Alerter l'électricien d'astreinte et le responsable HSE")
        )
        val evacSteps = listOf(
            ProcedureStepEntity(procedureId = "evacuation_generale", stepNumber = 1, title = "À l'alarme sonore, cesser immédiatement tout travail"),
            ProcedureStepEntity(procedureId = "evacuation_generale", stepNumber = 2, title = "Mettre les machines critiques en sécurité"),
            ProcedureStepEntity(procedureId = "evacuation_generale", stepNumber = 3, title = "Suivre le fléchage lumineux sans courir ni paniquer"),
            ProcedureStepEntity(procedureId = "evacuation_generale", stepNumber = 4, title = "Ne jamais utiliser les monte-charges ou ascenseurs"),
            ProcedureStepEntity(procedureId = "evacuation_generale", stepNumber = 5, title = "Rejoindre le Point de Rassemblement de votre zone"),
            ProcedureStepEntity(procedureId = "evacuation_generale", stepNumber = 6, title = "Répondre à l'appel du serre-file et attendre les consignes")
        )
        val fireSteps = listOf(
            ProcedureStepEntity(procedureId = "incendie_evacuation", stepNumber = 1, title = "Déclencher le bris de glace de l'alarme incendie"),
            ProcedureStepEntity(procedureId = "incendie_evacuation", stepNumber = 2, title = "Attaquer le feu naissant avec l'extincteur approprié"),
            ProcedureStepEntity(procedureId = "incendie_evacuation", stepNumber = 3, title = "Fermer portes et fenêtres pour limiter le tirage d'air"),
            ProcedureStepEntity(procedureId = "incendie_evacuation", stepNumber = 4, title = "Évacuer dans le calme vers les issues de secours"),
            ProcedureStepEntity(procedureId = "incendie_evacuation", stepNumber = 5, title = "Guider les sapeurs-pompiers vers les bouches d'incendie"),
            ProcedureStepEntity(procedureId = "incendie_evacuation", stepNumber = 6, title = "Interdire formellement tout retour dans le bâtiment")
        )
        val firstAidSteps = listOf(
            ProcedureStepEntity(procedureId = "premiers_secours", stepNumber = 1, title = "Protéger : éliminer le danger immédiat pour la victime"),
            ProcedureStepEntity(procedureId = "premiers_secours", stepNumber = 2, title = "Examiner la conscience et la respiration de la personne"),
            ProcedureStepEntity(procedureId = "premiers_secours", stepNumber = 3, title = "Faire alerter les secours (Infirmerie ext. 100)"),
            ProcedureStepEntity(procedureId = "premiers_secours", stepNumber = 4, title = "Mettre en Position Latérale de Sécurité (PLS) si inconscient"),
            ProcedureStepEntity(procedureId = "premiers_secours", stepNumber = 5, title = "Pratiquer la réanimation cardiopulmonaire (DAE disponible)"),
            ProcedureStepEntity(procedureId = "premiers_secours", stepNumber = 6, title = "Surveiller les constantes jusqu'à l'arrivée des secours")
        )
        dao.insertProcedureSteps(accidentSteps + chemSteps + elecSteps + evacSteps + fireSteps + firstAidSteps)

        // Seed Trainings matching screenshot (Pic 11)
        dao.insertTrainings(
            listOf(
                TrainingEntity(
                    id = "tr_caces",
                    title = "Travail en Hauteur — CACES",
                    instructor = "Jean-Pierre MVONDO",
                    site = "Figuil",
                    durationHours = 8,
                    dayNumber = "4",
                    monthShort = "AOÛT",
                    enrolledCount = 0,
                    maxCapacity = 15,
                    status = "Planifié",
                    isEnrolled = false
                ),
                TrainingEntity(
                    id = "tr_elec",
                    title = "Habilitation Électrique B1V/H1V",
                    instructor = "Emmanuel BIYA",
                    site = "Figuil",
                    durationHours = 16,
                    dayNumber = "18",
                    monthShort = "AOÛT",
                    enrolledCount = 6,
                    maxCapacity = 12,
                    status = "Planifié",
                    isEnrolled = true
                ),
                TrainingEntity(
                    id = "tr_sst",
                    title = "Sauveteur Secouriste du Travail (SST)",
                    instructor = "Dr. Angeline NKOA",
                    site = "Bonabéri",
                    durationHours = 14,
                    dayNumber = "25",
                    monthShort = "AOÛT",
                    enrolledCount = 10,
                    maxCapacity = 15,
                    status = "Planifié",
                    isEnrolled = false
                ),
                TrainingEntity(
                    id = "tr_loto",
                    title = "Consignation & Déconsignation (LOTO)",
                    instructor = "Marc ATANGANA",
                    site = "Nomayos",
                    durationHours = 7,
                    dayNumber = "2",
                    monthShort = "SEPT",
                    enrolledCount = 4,
                    maxCapacity = 10,
                    status = "Planifié",
                    isEnrolled = false
                )
            )
        )

        // Seed Events matching screenshot (Pic 10)
        dao.insertEvents(
            listOf(
                EventEntity(
                    id = "ev_world_day",
                    title = "Journée Mondiale de la Sécurité",
                    tag = "Journée Sécurité",
                    status = "Planifié",
                    dateText = "28 avr. 2026",
                    description = "Célébration annuelle du 28 avril — Journée mondiale de la Sécurité et de la Santé au Travail avec ateliers interactifs et challenges inter-équipes."
                ),
                EventEntity(
                    id = "ev_zero_acc",
                    title = "Campagne Vision Zéro Accident",
                    tag = "Campagne",
                    status = "En cours",
                    dateText = "T3 2026",
                    description = "Campagne de sensibilisation trimestrielle sur l'objectif zéro accident du groupe Holcim."
                ),
                EventEntity(
                    id = "ev_env_week",
                    title = "Semaine Éco-Responsabilité & Recyclage",
                    tag = "Environnement",
                    status = "Planifié",
                    dateText = "15 sept. 2026",
                    description = "Sensibilisation à la réduction de l'empreinte carbone, valorisation des coproduits et zéro déchet."
                )
            )
        )

        // Seed Notifications
        dao.insertNotification(
            NotificationEntity(
                title = "Alerte Météo & Fortes Chaleurs",
                message = "Températures élevées prévues sur le site de Figuil (38°C). Pauses hydratation obligatoires toutes les heures.",
                type = "Urgence",
                timestampText = "Il y a 30m",
                timestampMillis = System.currentTimeMillis() - 30 * 60 * 1000,
                isRead = false
            )
        )
        dao.insertNotification(
            NotificationEntity(
                title = "Nouvelle campagne lancée",
                message = "Participez au sondage satisfaction T3 2026 pour faire remonter vos suggestions.",
                type = "Information",
                timestampText = "Il y a 3h",
                timestampMillis = System.currentTimeMillis() - 3 * 3600 * 1000,
                isRead = false
            )
        )
        dao.insertNotification(
            NotificationEntity(
                title = "Rappel Formation Travail en Hauteur",
                message = "La session du 4 août sur le site de Figuil dispose encore de places ouvertes.",
                type = "Formation",
                timestampText = "Hier",
                timestampMillis = System.currentTimeMillis() - 24 * 3600 * 1000,
                isRead = true
            )
        )

        // Seed Surveys (Pic 14)
        dao.insertSurveys(
            listOf(
                SurveyEntity(
                    id = "survey_sat_t3",
                    title = "Sondage Satisfaction T3 2026",
                    description = "Partagez votre perception de la culture sécurité sur votre site pour le 3ème trimestre",
                    status = "Actif",
                    quarter = "T3 2026",
                    questionsCount = 5,
                    isCompleted = false
                ),
                SurveyEntity(
                    id = "survey_audit_comp",
                    title = "Audit Comportemental Sécurité",
                    description = "Évaluation des comportements sécuritaires au quotidien sur les sites CIMENCAM",
                    status = "Actif",
                    quarter = "T3 2026",
                    questionsCount = 6,
                    isCompleted = false
                ),
                SurveyEntity(
                    id = "survey_ergo",
                    title = "Évaluation Risques Ergonomiques",
                    description = "Retour sur les postes de travail sédentaires et de manutention manuelle",
                    status = "Actif",
                    quarter = "T3 2026",
                    questionsCount = 4,
                    isCompleted = true
                )
            )
        )


        // Seed Initial Content Entities (PDF, Reference Guides)
        dao.insertContents(
            listOf(
                ContentEntity(
                    id = "doc_pol_gen",
                    title = "Politique Générale HSE & Développement Durable",
                    description = "Vision, engagements stratégiques, règle fondamentale de droit de retrait et conformité ISO 45001 / 14001.",
                    type = "PDF",
                    category = "Politique de Sécurité",
                    fileUrl = "https://firebasestorage.googleapis.com/v0/b/app-hse/o/hse_documents%2Fpolitique_generale_2026.pdf?alt=media",
                    viewsCount = 142,
                    dateText = "Janv. 2026",
                    referenceCode = "POL-SEC-001",
                    version = "v4.0",
                    authorName = "Comité de Direction",
                    authorRole = "Direction Générale",
                    site = "Tous les sites",
                    timestampMillis = System.currentTimeMillis() - 60L * 24 * 3600 * 1000
                ),
                ContentEntity(
                    id = "doc_livret_accueil",
                    title = "Livret d'Accueil & Induction Sécurité",
                    description = "Manuel complet d'intégration pour les nouveaux arrivants, prestataires et visiteurs des usines et carrières.",
                    type = "PDF",
                    category = "Induction",
                    fileUrl = "https://firebasestorage.googleapis.com/v0/b/app-hse/o/hse_documents%2Flivret_accueil_hse.pdf?alt=media",
                    viewsCount = 89,
                    dateText = "Févr. 2026",
                    referenceCode = "LIV-IND-002",
                    version = "v3.2",
                    authorName = "Direction HSE",
                    authorRole = "Coordonnateur HSE",
                    site = "Tous les sites",
                    timestampMillis = System.currentTimeMillis() - 40L * 24 * 3600 * 1000
                ),
                ContentEntity(
                    id = "doc_loto",
                    title = "Standard Consignation & Déconsignation (LOTO)",
                    description = "Procédure d'isolation des énergies électrique, mécanique, pneumatique et hydraulique avant toute intervention.",
                    type = "PDF",
                    category = "Sécurité",
                    fileUrl = "https://firebasestorage.googleapis.com/v0/b/app-hse/o/hse_documents%2Fstandard_loto_consignation.pdf?alt=media",
                    viewsCount = 215,
                    dateText = "Mars 2026",
                    referenceCode = "STD-LOTO-003",
                    version = "v2.1",
                    authorName = "Marc Atangana",
                    authorRole = "Responsable Maintenance",
                    site = "Nomayos",
                    timestampMillis = System.currentTimeMillis() - 25L * 24 * 3600 * 1000
                ),
                ContentEntity(
                    id = "doc_hauteur",
                    title = "Guide Pratique Travaux en Hauteur & Échafaudages",
                    description = "Règles d'utilisation du harnais de sécurité double longe, vérification des points d'ancrage et échafaudages conformes.",
                    type = "PDF",
                    category = "Sécurité",
                    fileUrl = "https://firebasestorage.googleapis.com/v0/b/app-hse/o/hse_documents%2Fguide_travaux_hauteur.pdf?alt=media",
                    viewsCount = 178,
                    dateText = "Mars 2026",
                    referenceCode = "GUI-HAUT-004",
                    version = "v2.0",
                    authorName = "Pôle Prévention",
                    authorRole = "Ingénieur Sécurité",
                    site = "Figuil",
                    timestampMillis = System.currentTimeMillis() - 20L * 24 * 3600 * 1000
                ),
                ContentEntity(
                    id = "doc_sante_travail",
                    title = "Protocole Santé au Travail & Ergonomie",
                    description = "Prévention des risques biomécaniques, postures de travail adaptées et surveillance médicale renforcée.",
                    type = "PDF",
                    category = "Santé",
                    fileUrl = "https://firebasestorage.googleapis.com/v0/b/app-hse/o/hse_documents%2Fprotocole_sante_ergonomie.pdf?alt=media",
                    viewsCount = 96,
                    dateText = "Avr. 2026",
                    referenceCode = "PROT-SAN-005",
                    version = "v1.5",
                    authorName = "Dr. Angeline Nkoa",
                    authorRole = "Médecin du Travail",
                    site = "Tous les sites",
                    timestampMillis = System.currentTimeMillis() - 15L * 24 * 3600 * 1000
                ),
                ContentEntity(
                    id = "doc_environnement",
                    title = "Charte Éco-Responsabilité & Zéro Déchet",
                    description = "Engagements environnementaux, tri sélectif des déchets industriels et réduction des émissions de CO2.",
                    type = "PDF",
                    category = "Environnement",
                    fileUrl = "https://firebasestorage.googleapis.com/v0/b/app-hse/o/hse_documents%2Fcharte_environnement_2026.pdf?alt=media",
                    viewsCount = 112,
                    dateText = "Mai 2026",
                    referenceCode = "ENV-CHA-006",
                    version = "v1.2",
                    authorName = "Alain Mbarga",
                    authorRole = "Responsable Environnement",
                    site = "Bonabéri",
                    timestampMillis = System.currentTimeMillis() - 10L * 24 * 3600 * 1000
                ),
                ContentEntity(
                    id = "doc_secours",
                    title = "Manuel d'Intervention Premiers Secours",
                    description = "Gestes d'urgence vitale, utilisation du défibrillateur automatisé externe (DAE) et chaîne d'alerte des secours.",
                    type = "PDF",
                    category = "Santé",
                    fileUrl = "https://firebasestorage.googleapis.com/v0/b/app-hse/o/hse_documents%2Fmanuel_premiers_secours.pdf?alt=media",
                    viewsCount = 204,
                    dateText = "Juin 2026",
                    referenceCode = "MAN-SEC-007",
                    version = "v3.0",
                    authorName = "Service Médical",
                    authorRole = "Infirmier de Site",
                    site = "Tous les sites",
                    timestampMillis = System.currentTimeMillis() - 5L * 24 * 3600 * 1000
                )
            )
        )

        // Seed 25 Users (Super Admin, Admins, Managers, Collaborateurs) matching screenshot Gest contenu 5
        dao.insertUsers(
            listOf(
                UserEntity(
                    email = "abeissajean66@gmail.com",
                    name = "Jean Abeissa (Super Admin)",
                    role = "Admin",
                    site = "Direction Générale (Tous sites)",
                    department = "Direction Générale",
                    phone = "+237 690 00 00 00",
                    avatarInitials = "JA",
                    status = "Actif"
                ),
                UserEntity(
                    email = "admin@holcim.cm",
                    name = "Administrateur Principal",
                    role = "Admin",
                    site = "Siège Douala",
                    department = "Direction Générale",
                    phone = "+237 690 00 00 01",
                    avatarInitials = "AP",
                    status = "Actif"
                ),
                UserEntity(
                    email = "admin@cimencam.cm",
                    name = "Marie Dupont",
                    role = "Admin",
                    site = "Bonabéri",
                    department = "Direction",
                    phone = "+237 690 00 00 02",
                    avatarInitials = "MD",
                    status = "Actif"
                ),
                UserEntity(
                    email = "manager@holcim.cm",
                    name = "Jean-Pierre Mvondo",
                    role = "Manager",
                    site = "Figuil",
                    department = "Production Four",
                    phone = "+237 690 00 00 03",
                    avatarInitials = "JM",
                    status = "Actif"
                ),
                UserEntity(
                    email = "paulin.mbassi@cimencam.cm",
                    name = "Paulin Mbassi",
                    role = "Manager",
                    site = "Bonabéri",
                    department = "Direction Usine",
                    phone = "+237 690 00 00 04",
                    avatarInitials = "PM",
                    status = "Actif"
                ),
                UserEntity(
                    email = "marc.atangana@cimencam.cm",
                    name = "Marc Atangana",
                    role = "Manager",
                    site = "Nomayos",
                    department = "Maintenance Électrique",
                    phone = "+237 690 00 00 05",
                    avatarInitials = "MA",
                    status = "Actif"
                ),
                UserEntity(
                    email = "angeline.nkoa@cimencam.cm",
                    name = "Dr. Angeline Nkoa",
                    role = "Manager",
                    site = "Tous sites",
                    department = "Santé au Travail",
                    phone = "+237 690 00 00 06",
                    avatarInitials = "AN",
                    status = "Actif"
                ),
                UserEntity(
                    email = "oumarou.sali@cimencam.cm",
                    name = "Cdt. Oumarou Sali",
                    role = "Manager",
                    site = "Figuil",
                    department = "Sécurité Incendie",
                    phone = "+237 690 00 00 07",
                    avatarInitials = "OS",
                    status = "Actif"
                ),
                UserEntity(
                    email = "abeissajean66@gmail.com",
                    name = "Jean Abeissa",
                    role = "Collaborateur",
                    site = "Figuil",
                    department = "Sécurité Industrielle",
                    phone = "+237 699 12 34 56",
                    avatarInitials = "JA",
                    status = "Actif"
                ),
                UserEntity(
                    email = "alain.mbarga@cimencam.cm",
                    name = "Alain Mbarga",
                    role = "Collaborateur",
                    site = "Bonabéri",
                    department = "Environnement",
                    phone = "+237 691 22 33 44",
                    avatarInitials = "AM",
                    status = "Actif"
                ),
                UserEntity(
                    email = "emmanuel.biya@cimencam.cm",
                    name = "Emmanuel Biya",
                    role = "Collaborateur",
                    site = "Nomayos",
                    department = "Ensachage",
                    phone = "+237 692 33 44 55",
                    avatarInitials = "EB",
                    status = "Actif"
                ),
                UserEntity(
                    email = "fabrice.ondoa@cimencam.cm",
                    name = "Fabrice Ondoa",
                    role = "Collaborateur",
                    site = "Nomayos",
                    department = "Maintenance",
                    phone = "+237 693 44 55 66",
                    avatarInitials = "FO",
                    status = "Actif"
                ),
                UserEntity(
                    email = "samuel.beleck@cimencam.cm",
                    name = "Samuel Beleck",
                    role = "Collaborateur",
                    site = "Figuil",
                    department = "Carrière",
                    phone = "+237 694 55 66 77",
                    avatarInitials = "SB",
                    status = "Actif"
                ),
                UserEntity(
                    email = "emmanuel.tchuenkam@cimencam.cm",
                    name = "Emmanuel Tchuenkam",
                    role = "Collaborateur",
                    site = "Bonabéri",
                    department = "Broyeur",
                    phone = "+237 695 66 77 88",
                    avatarInitials = "ET",
                    status = "Actif"
                ),
                UserEntity(
                    email = "joseph.ebanda@cimencam.cm",
                    name = "Joseph Ebanda",
                    role = "Collaborateur",
                    site = "Figuil",
                    department = "Laboratoire Qualité",
                    phone = "+237 696 77 88 99",
                    avatarInitials = "JE",
                    status = "Actif"
                ),
                UserEntity(
                    email = "cedric.tamgno@cimencam.cm",
                    name = "Cédric Tamgno",
                    role = "Collaborateur",
                    site = "Bonabéri",
                    department = "Logistique & Expédition",
                    phone = "+237 697 88 99 00",
                    avatarInitials = "CT",
                    status = "Actif"
                ),
                UserEntity(
                    email = "boris.mballa@cimencam.cm",
                    name = "Boris Mballa",
                    role = "Collaborateur",
                    site = "Nomayos",
                    department = "Centrale Thermique",
                    phone = "+237 698 99 00 11",
                    avatarInitials = "BM",
                    status = "Actif"
                ),
                UserEntity(
                    email = "nathalie.kouam@cimencam.cm",
                    name = "Nathalie Kouam",
                    role = "Collaborateur",
                    site = "Siège Douala",
                    department = "Ressources Humaines",
                    phone = "+237 699 00 11 22",
                    avatarInitials = "NK",
                    status = "Actif"
                ),
                UserEntity(
                    email = "christian.eyike@cimencam.cm",
                    name = "Christian Eyike",
                    role = "Collaborateur",
                    site = "Figuil",
                    department = "Électromécanique",
                    phone = "+237 670 11 22 33",
                    avatarInitials = "CE",
                    status = "Actif"
                ),
                UserEntity(
                    email = "rodrigue.nguemo@cimencam.cm",
                    name = "Rodrigue Nguemo",
                    role = "Collaborateur",
                    site = "Bonabéri",
                    department = "Contrôle Commande",
                    phone = "+237 671 22 33 44",
                    avatarInitials = "RN",
                    status = "Actif"
                ),
                UserEntity(
                    email = "arlette.bikoue@cimencam.cm",
                    name = "Arlette Bikoué",
                    role = "Collaborateur",
                    site = "Nomayos",
                    department = "Approvisionnement",
                    phone = "+237 672 33 44 55",
                    avatarInitials = "AB",
                    status = "Actif"
                ),
                UserEntity(
                    email = "herve.kamga@cimencam.cm",
                    name = "Hervé Kamga",
                    role = "Collaborateur",
                    site = "Figuil",
                    department = "Concasseur",
                    phone = "+237 673 44 55 66",
                    avatarInitials = "HK",
                    status = "Actif"
                ),
                UserEntity(
                    email = "yves.talla@cimencam.cm",
                    name = "Yves Talla",
                    role = "Collaborateur",
                    site = "Bonabéri",
                    department = "Silos & Vrac",
                    phone = "+237 674 55 66 77",
                    avatarInitials = "YT",
                    status = "En attente"
                ),
                UserEntity(
                    email = "sandrine.ebongue@cimencam.cm",
                    name = "Sandrine Ebongue",
                    role = "Collaborateur",
                    site = "Nomayos",
                    department = "Qualité & Process",
                    phone = "+237 675 66 77 88",
                    avatarInitials = "SE",
                    status = "En attente"
                ),
                UserEntity(
                    email = "eric.messina@cimencam.cm",
                    name = "Eric Messina",
                    role = "Collaborateur",
                    site = "Siège Douala",
                    department = "Audit Interne",
                    phone = "+237 676 77 88 99",
                    avatarInitials = "EM",
                    status = "Inactif"
                )
            )
        )

    }
}
