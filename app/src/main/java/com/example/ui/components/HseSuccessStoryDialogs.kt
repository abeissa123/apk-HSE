package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.entity.SuccessStoryEntity
import com.example.domain.HseImageAnnotation
import com.example.domain.HseImageMedia
import com.example.domain.HseVideoChapter
import com.example.domain.HseVideoMedia
import com.example.ui.theme.*

// ============================================================================
// READING THEME FOR HSE SUCCESS STORIES & TESTIMONIALS
// ============================================================================
enum class HseStoryReadingTheme(
    val label: String,
    val canvasBg: Color,
    val sheetBg: Color,
    val sheetBorder: Color,
    val primaryText: Color,
    val secondaryText: Color,
    val mutedText: Color,
    val accent: Color,
    val calloutBg: Color,
    val calloutBorder: Color,
    val warningBg: Color,
    val warningBorder: Color,
    val warningText: Color,
    val successBg: Color,
    val successBorder: Color,
    val successText: Color,
    val watermarkColor: Color
) {
    STUDIO_WHITE(
        label = "Blanc Studio",
        canvasBg = Color(0xFF1E293B),
        sheetBg = Color(0xFFFFFFFF),
        sheetBorder = Color(0xFFE2E8F0),
        primaryText = Color(0xFF0F172A),
        secondaryText = Color(0xFF475569),
        mutedText = Color(0xFF94A3B8),
        accent = Color(0xFF1E40AF),
        calloutBg = Color(0xFFEFF6FF),
        calloutBorder = Color(0xFFBFDBFE),
        warningBg = Color(0xFFFEF2F2),
        warningBorder = Color(0xFFDC2626),
        warningText = Color(0xFF991B1B),
        successBg = Color(0xFFECFDF5),
        successBorder = Color(0xFF059669),
        successText = Color(0xFF065F46),
        watermarkColor = Color(0x0A000000)
    ),
    WARM_SEPIA(
        label = "Sépia Confort",
        canvasBg = Color(0xFF292524),
        sheetBg = Color(0xFFFFFDF5),
        sheetBorder = Color(0xFFE7E5E4),
        primaryText = Color(0xFF292524),
        secondaryText = Color(0xFF57534E),
        mutedText = Color(0xFF78716C),
        accent = Color(0xFFB45309),
        calloutBg = Color(0xFFFEF3C7),
        calloutBorder = Color(0xFFFDE68A),
        warningBg = Color(0xFFFFF1F2),
        warningBorder = Color(0xFFE11D48),
        warningText = Color(0xFF9F1239),
        successBg = Color(0xFFF0FDF4),
        successBorder = Color(0xFF16A34A),
        successText = Color(0xFF166534),
        watermarkColor = Color(0x0C000000)
    ),
    DARK_PRO(
        label = "Sombre Nuit Pro",
        canvasBg = Color(0xFF020617),
        sheetBg = Color(0xFF0F172A),
        sheetBorder = Color(0xFF1E293B),
        primaryText = Color(0xFFF8FAFC),
        secondaryText = Color(0xFF94A3B8),
        mutedText = Color(0xFF64748B),
        accent = Color(0xFF38BDF8),
        calloutBg = Color(0xFF1E293B),
        calloutBorder = Color(0xFF334155),
        warningBg = Color(0xFF450A0A),
        warningBorder = Color(0xFFDC2626),
        warningText = Color(0xFFFCA5A5),
        successBg = Color(0xFF064E3B),
        successBorder = Color(0xFF10B981),
        successText = Color(0xFF6EE7B7),
        watermarkColor = Color(0x0DFFFFFF)
    ),
    OLED_BLACK(
        label = "Noir OLED Éco",
        canvasBg = Color(0xFF000000),
        sheetBg = Color(0xFF000000),
        sheetBorder = Color(0xFF27272A),
        primaryText = Color(0xFFFAFAFA),
        secondaryText = Color(0xFFA1A1AA),
        mutedText = Color(0xFF71717A),
        accent = Color(0xFF0284C7),
        calloutBg = Color(0xFF18181B),
        calloutBorder = Color(0xFF27272A),
        warningBg = Color(0xFF2A0000),
        warningBorder = Color(0xFFEF4444),
        warningText = Color(0xFFF87171),
        successBg = Color(0xFF022C22),
        successBorder = Color(0xFF059669),
        successText = Color(0xFF34D399),
        watermarkColor = Color(0x10FFFFFF)
    )
}

// ============================================================================
// DATA MODEL FOR RICH TESTIMONIALS
// ============================================================================
data class HseTestimonialDetail(
    val id: String,
    val title: String,
    val quote: String,
    val authorName: String,
    val authorRole: String,
    val site: String,
    val dateText: String,
    val readingTimeMinutes: Int = 3,
    val categoryTag: String = "Retour d'Expérience (REX)",
    val kpiImpacts: List<Pair<String, String>>,
    val contextSection: String,
    val actionsSection: List<String>,
    val resultsSection: String,
    val adviceToColleagues: String,
    val isoStandard: String = "ISO 45001:2018 & ISO 14001:2015",
    val likesCount: Int = 35,
    val isLiked: Boolean = false
)

// ============================================================================
// MAPPER UTILITIES
// ============================================================================
object HseSuccessStoryMapper {

    fun toVideoMedia(story: SuccessStoryEntity): HseVideoMedia {
        return when (story.id) {
            "story_video_rex_fire" -> HseVideoMedia(
                id = story.id,
                title = story.title,
                subtitle = "Exercice d'intervention d'urgence et manœuvre des lances incendie",
                category = "Entraînement Opérationnel",
                durationText = "02:48",
                durationSeconds = 168,
                resolution = "1080p Full HD",
                presenter = "${story.authorName} (${story.authorRole})",
                site = "Site de ${story.site}",
                viewsCount = 384,
                isCertificateEligible = true,
                summary = "Simulation chronométrée d'un départ de feu au parc à matières avec déploiement des RIA et évacuation générale.",
                chapters = listOf(
                    HseVideoChapter("Déclenchement alarme & évacuation", 0, "00:00", "Sirène générale et rassemblement au point A"),
                    HseVideoChapter("Déploiement équipe première intervention", 45, "00:45", "Raccordement des RIA et vérification de la pression"),
                    HseVideoChapter("Extinction simulée et sécurisation", 90, "01:30", "Attaque de la nappe et extinction"),
                    HseVideoChapter("Débriefing & Chronométrage", 135, "02:15", "Temps total d'évacuation validé à 2 min 48 s")
                ),
                keyPoints = listOf(
                    "Temps d'évacuation total : 2 min 48 s (Objectif < 3 min atteint)",
                    "100% des collaborateurs présents ont rejoint le point de rassemblement sans encombre",
                    "Efficacité immédiate de l'équipe de première intervention (EPI)"
                ),
                accentColorHex = 0xFFDC2626
            )
            "story_video_zero_accident" -> HseVideoMedia(
                id = story.id,
                title = story.title,
                subtitle = "Célébration des 365 jours sans accident avec arrêt à l'Usine de Bonabéri",
                category = "Culture & Célébration HSE",
                durationText = "03:15",
                durationSeconds = 195,
                resolution = "4K UHD 60fps",
                presenter = "${story.authorName} (${story.authorRole})",
                site = "Site de ${story.site}",
                viewsCount = 612,
                isCertificateEligible = true,
                summary = "Rétrospective des actions de prévention, des causeries 5 minutes et des audits croisés ayant permis d'atteindre 1 an sans accident.",
                chapters = listOf(
                    HseVideoChapter("Introduction Direction Générale", 0, "00:00", "Félicitations aux 350 collaborateurs"),
                    HseVideoChapter("Rétrospective des initiatives clés", 50, "00:50", "Causeries 5 min, audits croisés et vigilance partagée"),
                    HseVideoChapter("Remise officielle du Trophée HSE", 100, "01:40", "Cérémonie symbolique et engagement 2027")
                ),
                keyPoints = listOf(
                    "365 jours consécutifs avec zéro accident du travail avec ou sans arrêt",
                    "Plus de 1 200 situations dangereuses remontées et traitées proactivement",
                    "Adhésion totale de la hiérarchie et des opérateurs au standard Zéro Dommage"
                ),
                accentColorHex = 0xFF2563EB
            )
            else -> HseVideoMedia(
                id = story.id,
                title = story.title,
                subtitle = story.description,
                category = "Success Story HSE",
                durationText = "02:00",
                durationSeconds = 120,
                resolution = "1080p Full HD",
                presenter = "${story.authorName} (${story.authorRole})",
                site = "Site de ${story.site}",
                viewsCount = story.likesCount * 10,
                isCertificateEligible = true,
                summary = story.description,
                chapters = listOf(
                    HseVideoChapter("Présentation de l'initiative", 0, "00:00", "Mise en place sur le site de ${story.site}"),
                    HseVideoChapter("Déploiement sur le terrain", 40, "00:40", "Action menée par ${story.authorName}"),
                    HseVideoChapter("Résultats et enseignements", 75, "01:15", "Bilan sécurité et perspectives")
                ),
                keyPoints = listOf(
                    "Initiative menée avec succès sur le site de ${story.site}",
                    "Impact direct sur la réduction des risques d'accident",
                    "Bonne pratique recommandée pour l'ensemble des sites CIMENCAM"
                ),
                accentColorHex = 0xFF059669
            )
        }
    }

    fun toImageMedia(story: SuccessStoryEntity): HseImageMedia {
        return when (story.id) {
            "story_100_days" -> HseImageMedia(
                id = story.id,
                title = story.title,
                subtitle = "Four de Clinkérisation — Usine de Figuil",
                category = "Photo REX Terrain",
                site = "Site de Figuil (Nord)",
                dateText = story.dateText,
                author = "${story.authorName} (${story.authorRole})",
                resolution = "3840 x 2160 (4K UHD)",
                description = "L'équipe de production du four au complet célébrant le cap historique de 100 jours consécutifs sans accident. Tous équipés de leurs EPI réglementaires complets (casque à jugulaire, lunettes, gants de manutention et chaussures de sécurité S3).",
                themeColorHex = 0xFF059669,
                annotations = listOf(
                    HseImageAnnotation("EPI Lunettes Teintées", "Port systématique des lunettes de protection teintées anti-éblouissement", 0.22f, 0.30f, false),
                    HseImageAnnotation("Tableau Causeries 5 min", "Tableau de bord de suivi journalier des causeries sécurité 5 minutes", 0.50f, 0.48f, false),
                    HseImageAnnotation("Balisage Piétons", "Balisage rigoureux de la zone de circulation des chariots et piétons", 0.78f, 0.70f, false)
                ),
                technicalSpecs = listOf(
                    "Capacité du four" to "1 200 tonnes de clinker/jour",
                    "Effectif concerné" to "42 opérateurs et techniciens",
                    "Taux de port EPI" to "100% contrôlé",
                    "Nombre de causeries" to "100 sessions consécutives"
                )
            )
            "story_co2_reduction" -> HseImageMedia(
                id = story.id,
                title = story.title,
                subtitle = "Filtres à Manches & Broyeur — Usine de Bonabéri",
                category = "Environnement & Dépoussiérage",
                site = "Site de Bonabéri (Douala)",
                dateText = story.dateText,
                author = "${story.authorName} (${story.authorRole})",
                resolution = "3840 x 2160 (4K UHD)",
                description = "Inspection technique de la nouvelle rampe de captage des poussières et du système de filtration haute performance installé sur le broyeur de ciment.",
                themeColorHex = 0xFF0284C7,
                annotations = listOf(
                    HseImageAnnotation("Capteur Optique Opacité", "Nouveau capteur optique d'opacité en continu connecté à la supervision", 0.30f, 0.35f, false),
                    HseImageAnnotation("Membranes PTFE", "Manches filtrantes en membrane PTFE à très haute efficacité", 0.65f, 0.60f, false),
                    HseImageAnnotation("Passerelle Sécurisée", "Zone de maintenance sécurisée avec passerelle caillebotis antidérapante", 0.85f, 0.80f, false)
                ),
                technicalSpecs = listOf(
                    "Réduction émissions" to "-15% poussières totales",
                    "Valeur mesurée" to "< 8 mg/Nm³ (Norme légale < 20)",
                    "Économie énergie" to "120 MWh/an",
                    "Investissement" to "Plan Vert Holcim 2026"
                )
            )
            "story_epi_innovation" -> HseImageMedia(
                id = story.id,
                title = story.title,
                subtitle = "Silos & Tour de Préchauffage — Usine de Nomayos",
                category = "Innovation EPI & Hauteur",
                site = "Site de Nomayos (Yaoundé)",
                dateText = story.dateText,
                author = "${story.authorName} (${story.authorRole})",
                resolution = "3840 x 2160 (4K UHD)",
                description = "Démonstration pratique de réglage et d'ancrage des nouveaux harnais de sécurité 4 points avec longe double antichute et absorbeur d'énergie pour les interventions au sommet des silos.",
                themeColorHex = 0xFFD97706,
                annotations = listOf(
                    HseImageAnnotation("Point d'ancrage EN 795", "Point d'ancrage certifié EN 795 sur ligne de vie continue", 0.25f, 0.28f, false),
                    HseImageAnnotation("Mousquetons Triple Action", "Mousquetons automatiques aluminium à triple sécurité", 0.52f, 0.50f, false),
                    HseImageAnnotation("Puce RFID d'inspection", "Témoin d'usure et puce RFID d'inspection périodique semestrielle", 0.75f, 0.75f, false)
                ),
                technicalSpecs = listOf(
                    "Standard appliqué" to "Travaux en hauteur Niv. 2",
                    "Poids du harnais" to "1.3 kg (ergonomie allégée)",
                    "Résistance certifiée" to "22 kN",
                    "Formation reçue" to "100% des intervenants"
                )
            )
            else -> HseImageMedia(
                id = story.id,
                title = story.title,
                subtitle = "Site CIMENCAM de ${story.site}",
                category = "Photo REX HSE",
                site = "Site de ${story.site}",
                dateText = story.dateText,
                author = "${story.authorName} (${story.authorRole})",
                resolution = "3840 x 2160 (4K UHD)",
                description = story.description,
                themeColorHex = 0xFF1E40AF,
                annotations = listOf(
                    HseImageAnnotation("Bonne Pratique", "Zone d'application de la bonne pratique sécurité", 0.40f, 0.40f, false),
                    HseImageAnnotation("Règle Vitale", "Respect des 12 Règles Vitales Holcim", 0.70f, 0.65f, false)
                ),
                technicalSpecs = listOf(
                    "Site" to story.site,
                    "Auteur" to story.authorName,
                    "Date" to story.dateText
                )
            )
        }
    }

    fun toTestimonialDetail(story: SuccessStoryEntity): HseTestimonialDetail {
        return when (story.id) {
            "story_sst_20" -> HseTestimonialDetail(
                id = story.id,
                title = story.title,
                quote = "Former 20 nouveaux secouristes sur nos usines garantit qu'en cas d'urgence, chaque minute compte et chaque geste sauve une vie.",
                authorName = story.authorName,
                authorRole = story.authorRole,
                site = story.site,
                dateText = story.dateText,
                readingTimeMinutes = 3,
                categoryTag = "Santé au Travail & Secourisme",
                kpiImpacts = listOf(
                    "20" to "Secouristes certifiés",
                    "< 2 min" to "Délai max d'intervention",
                    "100%" to "Couverture des 3 postes",
                    "3 Sites" to "Figuil • Nomayos • Bonabéri"
                ),
                contextSection = "Dans les environnements industriels lourds tels que nos cimenteries et carrières, la promptitude des premiers secours avant l'arrivée du personnel médical ou des sapeurs-pompiers est un facteur déterminant pour prévenir l'aggravation d'un traumatisme ou gérer un malaise cardiaque. Le service médical a donc initié une campagne intensive de formation et de recyclage aux gestes qui sauvent.",
                actionsSection = listOf(
                    "Sélection de volontaires dans chaque équipe opérationnelle (poste matin, soir et nuit) pour garantir une présence 24h/24.",
                    "Programme complet de 16 heures dispensé par les instructeurs de la Croix-Rouge et notre médecin du travail.",
                    "Ateliers pratiques de Réanimation Cardio-Pulmonaire (RCP), pose de défibrillateur automatisé externe (DAE) et pose de garrot tourniquet.",
                    "Simulations surprises de scénarios réels (brûlure thermique, coupure, chute de plain-pied, inhalation de poussières)."
                ),
                resultsSection = "L'ensemble des 20 candidats ont validé avec succès l'examen final et ont reçu leur badge officiel SST ainsi qu'une trousse d'intervention rapide individuelle. Chaque atelier de broyage, four, ensachage et carrière dispose désormais d'au moins deux secouristes formés par équipe.",
                adviceToColleagues = "N'ayez pas peur de vous engager. Les gestes de premiers secours sont simples quand on les pratique régulièrement, et ils servent autant à l'usine qu'auprès de nos familles à la maison.",
                isoStandard = "ISO 45001:2018 §8.2 — Préparation et réponse aux situations d'urgence",
                likesCount = story.likesCount,
                isLiked = story.isLiked
            )
            "story_temoignage_rex_broyeur" -> HseTestimonialDetail(
                id = story.id,
                title = story.title,
                quote = "Quand j'ai vu la sangle du convoyeur frotter anormalement, je n'ai pas hésité une seconde : arrêt d'urgence immédiat et cadenas de consignation LOTO. La sécurité passe avant la cadence de production.",
                authorName = story.authorName,
                authorRole = story.authorRole,
                site = story.site,
                dateText = story.dateText,
                readingTimeMinutes = 4,
                categoryTag = "Retour d'Expérience • Consignation LOTO",
                kpiImpacts = listOf(
                    "0" to "Blessure corporelle",
                    "100%" to "Consignation LOTO",
                    "15 min" to "Temps de diagnostic",
                    "Règle Vital #2" to "Énergie Zéro appliquée"
                ),
                contextSection = "Lors du poste de nuit au broyeur à ciment de Bonabéri, un bruit anormal et un début d'échauffement ont été détectés le long du convoyeur à bande transporteur de clinker. Sous la pression des cadences, le réflexe d'antan aurait pu être de s'approcher pour inspecter la bande en mouvement.",
                actionsSection = listOf(
                    "Actionnement sans hésiter du câble d'arrêt d'urgence coupant instantanément la force motrice.",
                    "Application stricte de la procédure de consignation électrique et mécanique LOTO (cadenas personnel + étiquette nominative).",
                    "Vérification de l'absence de tension (VAT) et vidange des énergies résiduelles avant toute approche.",
                    "Appel du superviseur et de l'équipe mécanique pour dégager un corps étranger métallique coincé dans le tambour."
                ),
                resultsSection = "L'intervention s'est déroulée en toute sérénité. Aucun dommage corporel n'a été déploré et la bande transporteuse n'a pas subi de déchirure majeure, ce qui aurait pu paralyser l'usine pendant 48 heures.",
                adviceToColleagues = "La Direction nous donne explicitement le droit et le devoir de refuser ou d'interrompre tout travail jugé dangereux. Utilisez toujours ce droit de retrait : aucun sac de ciment ne vaut la vie d'un collègue.",
                isoStandard = "12 Règles Vitales Holcim • Règle #2 : Consignation et Isolement des Énergies",
                likesCount = story.likesCount,
                isLiked = story.isLiked
            )
            "story_temoignage_ambassadeur" -> HseTestimonialDetail(
                id = story.id,
                title = story.title,
                quote = "Les 5 minutes de causerie du matin ne sont pas une contrainte administrative : c'est notre rituel de fraternité et de concentration avant d'attaquer la roche.",
                authorName = story.authorName,
                authorRole = story.authorRole,
                site = story.site,
                dateText = story.dateText,
                readingTimeMinutes = 3,
                categoryTag = "Leadership & Vigilance Partagée",
                kpiImpacts = listOf(
                    "5 min" to "Rituel quotidien",
                    "32" to "Opérateurs carrière",
                    "0" to "Presqu'accident non signalé",
                    "100%" to "Adhésion de l'équipe"
                ),
                contextSection = "La carrière de calcaire de Figuil présente des contraintes fortes : chaleur extrême, coactivité d'engins miniers lourds (dampers 50 tonnes, pelles hydrauliques) et manipulation de tirs d'abattage. Pour maintenir un niveau de vigilance maximal, l'animation de causeries vivantes était indispensable.",
                actionsSection = listOf(
                    "Remplacement des longs discours théoriques par des analyses interactives de cas réels survenus la veille.",
                    "Donner la parole aux plus jeunes recrues pour qu'elles expriment leurs doutes sans crainte de jugement.",
                    "Mise en avant des félicitations publiques pour les opérateurs qui corrigent spontanément une situation dangereuse.",
                    "Vérification collective de l'état d'hydratation et du port des EPI avant le départ des engins."
                ),
                resultsSection = "Depuis 6 mois, le nombre de situations dangereuses remontées a augmenté de 40%, prouvant que la culture de transparence est installée. Zéro accident avec ou sans arrêt enregistré sur l'ensemble de la carrière.",
                adviceToColleagues = "Quand vous voyez un collègue faire une imprudence, ne soyez pas un simple spectateur. Allez lui parler avec bienveillance. La vigilance partagée, c'est prendre soin les uns des autres.",
                isoStandard = "ISO 45001:2018 §5.4 — Consultation et participation des travailleurs",
                likesCount = story.likesCount,
                isLiked = story.isLiked
            )
            else -> HseTestimonialDetail(
                id = story.id,
                title = story.title,
                quote = story.description,
                authorName = story.authorName,
                authorRole = story.authorRole,
                site = story.site,
                dateText = story.dateText,
                readingTimeMinutes = 3,
                categoryTag = "Success Story HSE • ${story.site}",
                kpiImpacts = listOf(
                    "Site" to story.site,
                    "Statut" to "Validé HSE",
                    "Impact" to "Positif",
                    "Règles" to "Respectées"
                ),
                contextSection = "Cette initiative s'inscrit dans notre volonté permanente d'amélioration continue et d'engagement de chacun pour faire de nos sites des lieux de travail toujours plus sûrs et sains.",
                actionsSection = listOf(
                    "Observation rigoureuse des conditions de travail et identification des pistes d'amélioration.",
                    "Mobilisation des équipes opérationnelles et concertation avec le service HSE.",
                    "Mise en œuvre concrète des actions de prévention et contrôle de leur efficacité dans le temps."
                ),
                resultsSection = "Les bénéfices sont concrets : meilleure ergonomie, réduction des risques identifiés et renforcement de l'esprit d'équipe.",
                adviceToColleagues = "Chaque initiative compte. Si vous avez une idée pour rendre votre poste de travail plus sûr, parlez-en à votre chef d'équipe ou à votre délégué sécurité.",
                isoStandard = "ISO 45001:2018 & 12 Règles Vitales Holcim",
                likesCount = story.likesCount,
                isLiked = story.isLiked
            )
        }
    }
}

// ============================================================================
// FULL-SCREEN SPACIOUS & RESPONSIVE HSE TESTIMONIAL READER DIALOG
// ============================================================================
@Composable
fun HseTestimonialReaderDialog(
    story: SuccessStoryEntity,
    onDismiss: () -> Unit,
    onToggleLike: (SuccessStoryEntity) -> Unit
) {
    val testimonial = remember(story) { HseSuccessStoryMapper.toTestimonialDetail(story) }
    var selectedTheme by remember { mutableStateOf(HseStoryReadingTheme.STUDIO_WHITE) }
    var zoomLevel by remember { mutableFloatStateOf(1.0f) }
    var isImmersiveMode by remember { mutableStateOf(false) }
    var showThemeMenu by remember { mutableStateOf(false) }
    var showZoomMenu by remember { mutableStateOf(false) }
    var showSecurityInfoDialog by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    // Reading progress calculation
    val readingProgress by remember {
        derivedStateOf {
            val totalItems = listState.layoutInfo.totalItemsCount
            if (totalItems <= 1) 0f
            else {
                val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                (lastVisible.toFloat() / (totalItems - 1).toFloat()).coerceIn(0f, 1f)
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(selectedTheme.canvasBg)
        ) {
            val screenWidth = maxWidth
            val isCompact = screenWidth < 380.dp
            val isTablet = screenWidth > 600.dp

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                // Top Action & Reader Control Bar
                if (!isImmersiveMode) {
                    Surface(
                        color = Color(0xFF0F172A),
                        shadowElevation = 8.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            // Progress bar
                            LinearProgressIndicator(
                                progress = { readingProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(3.dp),
                                color = Color(0xFF38BDF8),
                                trackColor = Color(0xFF1E293B)
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Left: Back Button & Title
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    IconButton(
                                        onClick = onDismiss,
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(Color.White.copy(alpha = 0.1f), CircleShape)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = "Retour",
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "Témoignage REX HSE",
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                color = Color(0xFF1E3A8A),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = "Site ${testimonial.site}",
                                                    color = Color(0xFF93C5FD),
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = "${testimonial.readingTimeMinutes} min de lecture • ${testimonial.categoryTag}",
                                            color = Color(0xFF94A3B8),
                                            fontSize = 10.5.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                // Right: Control Icons (Theme, Zoom, Immersive, Security)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Security Info Icon
                                    IconButton(
                                        onClick = { showSecurityInfoDialog = true },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = "Document Protégé",
                                            tint = Color(0xFF38BDF8),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    // Theme selector
                                    Box {
                                        IconButton(
                                            onClick = { showThemeMenu = true },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Palette,
                                                contentDescription = "Thème de lecture",
                                                tint = Color.White.copy(alpha = 0.85f),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        DropdownMenu(
                                            expanded = showThemeMenu,
                                            onDismissRequest = { showThemeMenu = false },
                                            modifier = Modifier.background(Color(0xFF1E293B))
                                        ) {
                                            HseStoryReadingTheme.entries.forEach { theme ->
                                                DropdownMenuItem(
                                                    text = {
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(16.dp)
                                                                    .background(theme.sheetBg, RoundedCornerShape(4.dp))
                                                                    .border(1.dp, Color.Gray, RoundedCornerShape(4.dp))
                                                            )
                                                            Spacer(modifier = Modifier.width(8.dp))
                                                            Text(
                                                                text = theme.label,
                                                                color = if (selectedTheme == theme) Color(0xFF60A5FA) else Color.White,
                                                                fontWeight = if (selectedTheme == theme) FontWeight.Bold else FontWeight.Normal,
                                                                fontSize = 13.sp
                                                            )
                                                        }
                                                    },
                                                    onClick = {
                                                        selectedTheme = theme
                                                        showThemeMenu = false
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    // Zoom selection
                                    Box {
                                        IconButton(
                                            onClick = { showZoomMenu = true },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.FormatSize,
                                                contentDescription = "Taille du texte",
                                                tint = Color.White.copy(alpha = 0.85f),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        DropdownMenu(
                                            expanded = showZoomMenu,
                                            onDismissRequest = { showZoomMenu = false },
                                            modifier = Modifier.background(Color(0xFF1E293B))
                                        ) {
                                            listOf(
                                                "85% (Compact)" to 0.85f,
                                                "100% (Standard)" to 1.0f,
                                                "115% (Confort)" to 1.15f,
                                                "130% (Grand)" to 1.30f
                                            ).forEach { (label, scale) ->
                                                DropdownMenuItem(
                                                    text = {
                                                        Text(
                                                            text = label,
                                                            color = if (zoomLevel == scale) Color(0xFF60A5FA) else Color.White,
                                                            fontWeight = if (zoomLevel == scale) FontWeight.Bold else FontWeight.Normal,
                                                            fontSize = 13.sp
                                                        )
                                                    },
                                                    onClick = {
                                                        zoomLevel = scale
                                                        showZoomMenu = false
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    // Fullscreen / Immersive Toggle
                                    IconButton(
                                        onClick = { isImmersiveMode = !isImmersiveMode },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Fullscreen,
                                            contentDescription = "Mode immersif",
                                            tint = Color.White.copy(alpha = 0.85f),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Main Reading Body (Spacious, Magazine Sheet)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .align(Alignment.TopCenter)
                            .widthIn(max = if (isTablet) 720.dp else 560.dp)
                            .padding(
                                horizontal = if (isCompact) 10.dp else if (isTablet) 24.dp else 16.dp,
                                vertical = 12.dp
                            ),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Document Sheet Card
                        item {
                            Surface(
                                color = selectedTheme.sheetBg,
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, selectedTheme.sheetBorder),
                                shadowElevation = 6.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(if (isCompact) 14.dp else 22.dp)
                                ) {
                                    // Header: Corporate Holcim • CIMENCAM Letterhead
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(8.dp)
                                                        .background(selectedTheme.accent, CircleShape)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "CIMENCAM • GROUPE HOLCIM",
                                                    color = selectedTheme.accent,
                                                    fontSize = (11 * zoomLevel).sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    letterSpacing = 1.sp
                                                )
                                            }
                                            Text(
                                                text = "DIRECTION HYGIÈNE, SÉCURITÉ & ENVIRONNEMENT",
                                                color = selectedTheme.secondaryText,
                                                fontSize = (9 * zoomLevel).sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }

                                        Surface(
                                            color = selectedTheme.calloutBg,
                                            shape = RoundedCornerShape(6.dp),
                                            border = BorderStroke(1.dp, selectedTheme.calloutBorder)
                                        ) {
                                            Text(
                                                text = "REX SÉCURITÉ",
                                                color = selectedTheme.accent,
                                                fontSize = (9 * zoomLevel).sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))
                                    HorizontalDivider(color = selectedTheme.sheetBorder.copy(alpha = 0.5f))
                                    Spacer(modifier = Modifier.height(14.dp))

                                    // Main Title
                                    Text(
                                        text = testimonial.title,
                                        color = selectedTheme.primaryText,
                                        fontSize = (20 * zoomLevel).sp,
                                        fontWeight = FontWeight.Bold,
                                        lineHeight = (26 * zoomLevel).sp
                                    )

                                    Spacer(modifier = Modifier.height(14.dp))

                                    // Highlight Callout Card (Quote)
                                    Surface(
                                        color = selectedTheme.calloutBg,
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, selectedTheme.calloutBorder),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(14.dp),
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Text(
                                                text = "❝",
                                                color = selectedTheme.accent,
                                                fontSize = (28 * zoomLevel).sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                lineHeight = (28 * zoomLevel).sp
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(
                                                text = testimonial.quote,
                                                color = selectedTheme.primaryText,
                                                fontSize = (13.5f * zoomLevel).sp,
                                                fontStyle = FontStyle.Italic,
                                                lineHeight = (20 * zoomLevel).sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    // Author & Metadata Box
                                    Surface(
                                        color = selectedTheme.canvasBg.copy(alpha = 0.04f),
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, selectedTheme.sheetBorder),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size((44 * zoomLevel).dp)
                                                    .background(
                                                        brush = Brush.linearGradient(
                                                            listOf(selectedTheme.accent, selectedTheme.accent.copy(alpha = 0.7f))
                                                        ),
                                                        shape = CircleShape
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = testimonial.authorName.take(2).uppercase(),
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = (15 * zoomLevel).sp
                                                )
                                            }

                                            Spacer(modifier = Modifier.width(12.dp))

                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = testimonial.authorName,
                                                        color = selectedTheme.primaryText,
                                                        fontSize = (14 * zoomLevel).sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Icon(
                                                        imageVector = Icons.Default.Verified,
                                                        contentDescription = "Vérifié",
                                                        tint = selectedTheme.accent,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
                                                Text(
                                                    text = "${testimonial.authorRole} • Site de ${testimonial.site}",
                                                    color = selectedTheme.secondaryText,
                                                    fontSize = (11 * zoomLevel).sp
                                                )
                                                Text(
                                                    text = "Publié le ${testimonial.dateText} • ${testimonial.readingTimeMinutes} min de lecture",
                                                    color = selectedTheme.mutedText,
                                                    fontSize = (10 * zoomLevel).sp
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    // KPI Impact Badges
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        testimonial.kpiImpacts.forEach { (value, label) ->
                                            Surface(
                                                color = selectedTheme.successBg,
                                                shape = RoundedCornerShape(8.dp),
                                                border = BorderStroke(1.dp, selectedTheme.successBorder),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 6.dp, vertical = 8.dp),
                                                    horizontalAlignment = Alignment.CenterHorizontally
                                                ) {
                                                    Text(
                                                        text = value,
                                                        color = selectedTheme.successText,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        fontSize = (12 * zoomLevel).sp
                                                    )
                                                    Text(
                                                        text = label,
                                                        color = selectedTheme.secondaryText,
                                                        fontSize = (9 * zoomLevel).sp,
                                                        textAlign = TextAlign.Center,
                                                        maxLines = 2,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(20.dp))

                                    // Section 1: Contexte Opérationnel
                                    TestimonialSectionHeader("01", "CONTEXTE & DÉFI OPÉRATIONNEL", selectedTheme, zoomLevel)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = testimonial.contextSection,
                                        color = selectedTheme.primaryText,
                                        fontSize = (13 * zoomLevel).sp,
                                        lineHeight = (20 * zoomLevel).sp
                                    )

                                    Spacer(modifier = Modifier.height(20.dp))

                                    // Section 2: Actions Clés Déployées
                                    TestimonialSectionHeader("02", "ACTIONS CONCRÈTES DÉPLOYÉES", selectedTheme, zoomLevel)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    testimonial.actionsSection.forEachIndexed { index, actionText ->
                                        Surface(
                                            color = selectedTheme.calloutBg,
                                            shape = RoundedCornerShape(8.dp),
                                            border = BorderStroke(1.dp, selectedTheme.calloutBorder),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(10.dp),
                                                verticalAlignment = Alignment.Top
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size((20 * zoomLevel).dp)
                                                        .background(selectedTheme.accent, CircleShape),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = "${index + 1}",
                                                        color = Color.White,
                                                        fontSize = (10 * zoomLevel).sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Text(
                                                    text = actionText,
                                                    color = selectedTheme.primaryText,
                                                    fontSize = (12.5f * zoomLevel).sp,
                                                    lineHeight = (18 * zoomLevel).sp
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(20.dp))

                                    // Section 3: Résultats & Bénéfices
                                    TestimonialSectionHeader("03", "RÉSULTATS & BÉNÉFICES MESURÉS", selectedTheme, zoomLevel)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = testimonial.resultsSection,
                                        color = selectedTheme.primaryText,
                                        fontSize = (13 * zoomLevel).sp,
                                        lineHeight = (20 * zoomLevel).sp
                                    )

                                    Spacer(modifier = Modifier.height(20.dp))

                                    // Section 4: Conseil d'Or de l'Auteur
                                    Surface(
                                        color = selectedTheme.calloutBg,
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, selectedTheme.calloutBorder),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(14.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.Lightbulb,
                                                    contentDescription = null,
                                                    tint = selectedTheme.accent,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "CONSEIL D'OR POUR LES ÉQUIPES",
                                                    color = selectedTheme.accent,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = (11 * zoomLevel).sp
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = testimonial.adviceToColleagues,
                                                color = selectedTheme.primaryText,
                                                fontSize = (13 * zoomLevel).sp,
                                                fontStyle = FontStyle.Italic,
                                                lineHeight = (19 * zoomLevel).sp
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(20.dp))

                                    // Section 5: Conformité & Standard ISO
                                    Surface(
                                        color = selectedTheme.warningBg,
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, selectedTheme.warningBorder),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.VerifiedUser,
                                                contentDescription = null,
                                                tint = selectedTheme.warningBorder,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    text = "RÉFÉRENTIEL AUDITÉ & CONFORME",
                                                    color = selectedTheme.warningText,
                                                    fontSize = (10 * zoomLevel).sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = testimonial.isoStandard,
                                                    color = selectedTheme.warningText,
                                                    fontSize = (11 * zoomLevel).sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(20.dp))

                                    // Endorsement & Signatures
                                    Surface(
                                        color = selectedTheme.calloutBg,
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, selectedTheme.sheetBorder),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(
                                                    text = "Visé par la Direction HSE :",
                                                    color = selectedTheme.accent,
                                                    fontSize = (10 * zoomLevel).sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = "Dr. Angeline NKOA • Médecin Travail CIMENCAM",
                                                    color = selectedTheme.primaryText,
                                                    fontSize = (11 * zoomLevel).sp
                                                )
                                            }

                                            Surface(
                                                color = selectedTheme.successBg,
                                                shape = RoundedCornerShape(6.dp),
                                                border = BorderStroke(1.dp, selectedTheme.successBorder)
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.CheckCircle,
                                                        contentDescription = null,
                                                        tint = selectedTheme.successBorder,
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = "CONFORME & APPROUVÉ",
                                                        color = selectedTheme.successText,
                                                        fontSize = (9 * zoomLevel).sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    // Subtle Watermark of Non-Exportability
                                    Text(
                                        text = "DOCUMENT INTERNE CIMENCAM • DIFFUSION STRICTE • CONSULTATION SÉCURISÉE SANS TÉLÉCHARGEMENT",
                                        color = selectedTheme.mutedText,
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }

                        // Interaction Bar (Likes & Safety Notice)
                        item {
                            Surface(
                                color = Color(0xFF0F172A),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, Color(0xFF1E293B)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Like Button
                                    Button(
                                        onClick = { onToggleLike(story) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (testimonial.isLiked) Color(0xFFEF4444) else Color(0xFF1E293B)
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (testimonial.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                            contentDescription = "Féliciter",
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (testimonial.isLiked) "${testimonial.likesCount} Félicité !" else "Féliciter (${testimonial.likesCount})",
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    // Close Button
                                    OutlinedButton(
                                        onClick = onDismiss,
                                        border = BorderStroke(1.dp, Color(0xFF334155)),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Fermer la lecture", color = Color(0xFF94A3B8), fontSize = 12.sp)
                                    }
                                }
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(20.dp))
                        }
                    }

                    // Exit Immersive Floating Pill
                    if (isImmersiveMode) {
                        Surface(
                            color = Color(0xDD0F172A),
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, Color(0xFF334155)),
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 16.dp)
                                .clickable { isImmersiveMode = false }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FullscreenExit,
                                    contentDescription = "Quitter le mode immersif",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Quitter l'immersion", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }

        // Security Info Dialog (DRM Non-Download Policy)
        if (showSecurityInfoDialog) {
            AlertDialog(
                onDismissRequest = { showSecurityInfoDialog = false },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(32.dp)
                    )
                },
                title = {
                    Text(
                        text = "Consultation Sécurisée Sans Téléchargement",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Les retours d'expérience, vidéos et photographies de la rubrique Success Stories HSE sont protégés par la politique de sécurité de l'information CIMENCAM (Groupe Holcim).",
                            color = Color(0xFFCBD5E1),
                            fontSize = 12.5.sp,
                            lineHeight = 17.sp
                        )
                        Surface(
                            color = Color(0xFF0F172A),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFF1E293B)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "🔒 Téléchargement et export désactivés\n" +
                                        "👁️ Consultation en streaming HD sur l'application\n" +
                                        "🛡️ Conformité stricte RGPD & Secret Industriel",
                                color = Color(0xFF93C5FD),
                                fontSize = 11.sp,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showSecurityInfoDialog = false }) {
                        Text("J'ai compris", color = Color(0xFF60A5FA), fontWeight = FontWeight.Bold)
                    }
                },
                containerColor = Color(0xFF1E293B)
            )
        }
    }
}

@Composable
private fun TestimonialSectionHeader(
    number: String,
    title: String,
    theme: HseStoryReadingTheme,
    zoomLevel: Float
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Surface(
            color = theme.accent,
            shape = RoundedCornerShape(4.dp)
        ) {
            Text(
                text = number,
                color = Color.White,
                fontSize = (9 * zoomLevel).sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            color = theme.accent,
            fontSize = (11.5f * zoomLevel).sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
    }
}
