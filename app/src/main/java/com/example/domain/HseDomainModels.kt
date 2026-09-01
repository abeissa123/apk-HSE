package com.example.domain

data class RealtimeWeatherData(
    val temperatureCelsius: Int = 30,
    val apparentTemperatureCelsius: Int = 32,
    val weatherCondition: String = "Ensoleillé",
    val weatherCode: Int = 0,
    val humidityPercent: Int = 65,
    val windSpeedKmh: Double = 10.0,
    val isLive: Boolean = false,
    val siteName: String = "Site de Figuil",
    val lastUpdated: String = "Temps réel"
)

data class HsePolicyCategory(
    val id: String,
    val title: String,
    val subtitle: String,
    val documentCount: Int,
    val iconName: String,
    val colorHex: Long
)

data class HseDocument(
    val id: String,
    val title: String,
    val subtitle: String,
    val category: String,
    val format: String = "PDF Officiel",
    val pagesCount: Int = 8,
    val referenceCode: String = "POL-SEC-2026",
    val version: String = "v3.2",
    val author: String = "Direction Développement Durable",
    val readingTimeMinutes: Int = 4,
    val summary: String = "Document de référence fixant les exigences obligatoires pour tous les collaborateurs et prestataires.",
    val content: String
)

data class SitePassagePlan(
    val id: String,
    val siteName: String,
    val title: String,
    val description: String,
    val accessPoints: List<String>,
    val musterPoints: List<String>,
    val infirmaryLocation: String,
    val emergencyNumber: String,
    val speedLimitKmH: Int = 20,
    val highRiskZones: List<String>
)

data class EmergencyFicheMeta(
    val id: String,
    val title: String,
    val subtitle: String,
    val iconType: String,
    val stepsCount: Int = 6,
    val accentColorHex: Long
)

data class HseVideoChapter(
    val title: String,
    val timestampSeconds: Int,
    val timestampFormatted: String,
    val description: String
)

data class HseVideoMedia(
    val id: String,
    val title: String,
    val subtitle: String,
    val category: String,
    val durationText: String = "08:45",
    val durationSeconds: Int = 525,
    val resolution: String = "1080p Full HD",
    val presenter: String = "M. E. Owona — Ingénieur Sécurité Industrielle",
    val site: String = "Tous sites (Figuil, Nomayos, Bonabéri)",
    val viewsCount: Int = 428,
    val isCertificateEligible: Boolean = true,
    val summary: String,
    val chapters: List<HseVideoChapter> = emptyList(),
    val keyPoints: List<String> = emptyList(),
    val accentColorHex: Long = 0xFF2563EB
)

data class HseImageAnnotation(
    val title: String,
    val description: String,
    val normalizedX: Float, // 0.0 to 1.0
    val normalizedY: Float, // 0.0 to 1.0
    val isDanger: Boolean = false
)

data class HseImageMedia(
    val id: String,
    val title: String,
    val subtitle: String,
    val category: String,
    val site: String,
    val dateText: String,
    val author: String = "Service Communication",
    val resolution: String = "3840 x 2160 (4K UHD)",
    val description: String,
    val themeColorHex: Long = 0xFF059669,
    val annotations: List<HseImageAnnotation> = emptyList(),
    val technicalSpecs: List<Pair<String, String>> = emptyList()
)


object HseStaticData {
    val categories = listOf(
        HsePolicyCategory(
            id = "politique_sec",
            title = "Politique de Sécurité",
            subtitle = "Vision & stratégie",
            documentCount = 1,
            iconName = "shield",
            colorHex = 0xFF2563EB
        ),
        HsePolicyCategory(
            id = "sante",
            title = "Santé",
            subtitle = "Bien-être au travail",
            documentCount = 1,
            iconName = "heart",
            colorHex = 0xFF10B981
        ),
        HsePolicyCategory(
            id = "securite",
            title = "Sécurité",
            subtitle = "Règles & procédures",
            documentCount = 1,
            iconName = "helmet",
            colorHex = 0xFFEF4444
        ),
        HsePolicyCategory(
            id = "environnement",
            title = "Environnement",
            subtitle = "Éco-responsabilité",
            documentCount = 1,
            iconName = "leaf",
            colorHex = 0xFF10B981
        ),
        HsePolicyCategory(
            id = "induction",
            title = "Induction",
            subtitle = "Nouveaux arrivants",
            documentCount = 2,
            iconName = "school",
            colorHex = 0xFF3B82F6
        )
    )

    val documents = listOf(
        HseDocument(
            id = "doc_pol_gen",
            title = "Politique Générale",
            subtitle = "Vision, mission et objectifs stratégiques",
            category = "Politique",
            format = "PDF Officiel",
            pagesCount = 6,
            referenceCode = "POL-SEC-001",
            version = "v4.0 - 2026",
            author = "Comité de Direction",
            readingTimeMinutes = 4,
            summary = "Déclaration d'engagement de la Direction Générale pour la santé, la sécurité au travail et le développement durable.",
            content = """
                # 1. VISION & ENGAGEMENT DE LA DIRECTION
                Chez CIMENCAM, filiale du Groupe Holcim, la Santé, la Sécurité et la préservation de l'Environnement constituent les fondations intangibles de l'ensemble de nos opérations industrielles. Aucune urgence de production, aucun impératif commercial ne saurait justifier de compromettre la sécurité d'un individu.

                ⚠️ RÈGLE FONDAMENTALE :
                Tout collaborateur a le droit et le DEVOIR d'interrompre immédiatement tout travail ou processus comportant un risque non maîtrisé pour lui-même ou pour autrui (Droit de retrait sans pénalité).

                # 2. NOS ENGAGEMENTS MAJEURS
                ✅ Zéro Accident Mortel et Zéro Accident avec Arrêt sur l'ensemble de nos usines et carrières (Figuil, Nomayos, Bonabéri).
                ✅ Intégrité physique et santé préservée pour chaque collaborateur, intérimaire et partenaire sous-traitant.
                ✅ Respect strict des standards internationaux ISO 45001 (Santé & Sécurité) et ISO 14001 (Environnement).
                ✅ Réduction continue de notre empreinte carbone et accélération de la transition vers l'économie circulaire.

                # 3. RESPONSABILITÉS & LEADERSHIP SUR LE TERRAIN
                Le leadership est l'affaire de tous les niveaux hiérarchiques :
                - Direction & Managers de Site : Exemplarité absolue, réalisation d'audits terrain hebdomadaires (VFL - Visible Felt Leadership).
                - Superviseurs de Quart : Animation du quart d'heure sécurité quotidien et contrôle systématique des autorisations de travail.
                - Collaborateurs & Opérateurs : Vigilance partagée, port systématique des EPI et signalement immédiat de tout presqu'accident via l'application mobile.

                # 4. SANCTIONS & DÉCLARATION
                Le non-respect délibéré des règles vitales de sécurité engage des mesures disciplinaires strictes conformément au règlement intérieur de l'entreprise.
            """.trimIndent()
        ),
        HseDocument(
            id = "doc_regles_or",
            title = "Règles d'Or de Sécurité",
            subtitle = "12 règles fondamentales de sécurité applicables sur tous les sites",
            category = "Sécurité",
            format = "PDF Officiel",
            pagesCount = 12,
            referenceCode = "REG-SEC-012",
            version = "v5.1 - 2026",
            author = "Département Sécurité Opérationnelle",
            readingTimeMinutes = 6,
            summary = "Les 12 exigences non négociables pour prévenir les risques critiques lors des travaux industriels.",
            content = """
                # 1. ÉQUIPEMENTS DE PROTECTION INDIVIDUELLE (EPI)
                Port obligatoire dès le franchissement de la zone industrielle : Casque de sécurité avec jugulaire, Chaussures de sécurité S3 à semelle anti-perforation, Lunettes de protection UV/poussière, Gilet haute visibilité classe 2 et Protections auditives en zones bruyantes (> 85 dB).

                # 2. CONSIGNATION & ISOLATION DES ÉNERGIES (LOTO)
                Neutralisation, cadenassage individuel et vérification d'absence totale d'énergie (électrique, pneumatique, hydraulique, thermique et gravitationnelle) avant toute opération d'entretien, de nettoyage ou de débourrage.

                # 3. TRAVAIL EN HAUTEUR
                Port obligatoire du harnais de sécurité avec double longe absorbeur de choc, raccordé à une ligne de vie ou point d'ancrage certifié dès 1,80 m de hauteur ou en bord de vide non protégé.

                # 4. ESPACES CONFINÉS & CUVE
                Intervention conditionnée à l'obtention préalable d'un permis d'entrée en espace confiné, contrôle de l'atmosphère (O2, CO, CO2, explosimètre), ventilation forcée continue et présence permanente d'un surveillant extérieur équipé de moyens d'alerte.

                # 5. CIRCULATION & CONDUITE D'ENGINS
                Vitesse strictement limitée à 20 km/h sur tout le site. Priorité absolue aux piétons sur les passages balisés. Port obligatoire de la ceinture. Interdiction formelle du téléphone portable au volant ou lors de la conduite de chariots et engins lourds.

                # 6. LEVAGE & MANUTENTION MÉCANIQUE
                Balisage rigoureux du rayon d'action de la charge. Interdiction absolue de circuler ou de stationner sous une charge suspendue. Vérification de la conformité des élingues, manilles et accessoires de levage.

                # 7. TRAVAUX À CHAUD & PERMIS DE FEU
                Permis de feu obligatoire pour toute soudure, découpage au chalumeau ou meulage. Élimination des matériaux combustibles dans un rayon de 10 m, extincteur à portée immédiate et ronde de surveillance 2 heures après la fin des travaux.

                # 8. GESTION DES PRODUITS CHIMIQUES
                Consultation obligatoire des Fiches de Données de Sécurité (FDS) avant manipulation. Port des gants et masques spécifiques. Utilisation des bacs de rétention et kits anti-déversement disponibles en station.

                # 9. DÉGAGEMENT DES VOIES D'ÉVACUATION
                Interdiction stricte et absolue d'encombrer les portes de secours, les couloirs d'évacuation, les douches de sécurité, les extincteurs et les RIA.

                # 10. TOLÉRANCE ZÉRO ALCOOL & SUBSTANCES
                Interdiction formelle d'introduction, de détention ou de consommation d'alcool, de drogues ou de substances altérant la vigilance sur l'ensemble des sites de l'entreprise.

                # 11. DROIT D'ALERTE ET DE RETRAIT
                Tout agent constatant un risque grave et imminent pour sa santé ou celle de ses collègues a l'obligation de stopper le travail et d'avertir immédiatement son chef d'équipe.

                # 12. SIGNALEMENT SYSTÉMATIQUE D'INCIDENTS
                Déclaration instantanée de tout accident, incident ou presqu'accident dans l'application sous 2 heures pour analyse des causes racines et partage du retour d'expérience.
            """.trimIndent()
        ),
        HseDocument(
            id = "doc_prev_sanitaire",
            title = "Guide de Prévention Sanitaire",
            subtitle = "Procédures et recommandations pour la prévention des risques sanitaires",
            category = "Santé",
            format = "PDF Officiel",
            pagesCount = 8,
            referenceCode = "SAN-PRO-2026",
            version = "v3.0 - 2026",
            author = "Service Médical",
            readingTimeMinutes = 5,
            summary = "Protocoles de prévention respiratoire, gestion des chaleurs intenses et ergonomie des postes.",
            content = """
                # 1. PROTECTION RESPIRATOIRE & POUSSIÈRES
                L'exposition aux poussières de clinker, de calcaire et de ciment nécessite une vigilance quotidienne :
                - Port des masques filtrants FFP2 / FFP3 obligatoires dans les zones de broyage, d'ensachage et de transfert.
                - Remplacement quotidien des masques à usage unique et nettoyage régulier des demi-masques à cartouche.
                - Maintenance préventive hebdomadaire des filtres et dépoussiéreurs industriels.

                # 2. PRÉVENTION DU STRESS THERMIQUE (GRANDES CHALEURS)
                Sur les sites exposés à de fortes températures (notamment Figuil et Nomayos) :
                ⚠️ RECOMMANDATIONS ESSENTIELLES :
                - Hydratation minimale de 2,5 à 3 litres d'eau potable par jour (disponible aux fontaines réfrigérées).
                - Pauses obligatoires de récupération à l'ombre ou dans les salles de repos climatisées.
                - Reconnaissance rapide des symptômes du coup de chaleur : vertiges, nausées, maux de tête intenses, peau sèche et chaude.

                # 3. ERGONOMIE & MANUTENTION DE CHARGES
                - Charge maximale autorisée pour une personne : 25 kg.
                - Utilisation prioritaire des moyens mécaniques (diables, palans, convoyeurs).
                - Posture sécuritaire : plier les genoux, garder le dos droit et maintenir la charge près du corps.

                # 4. SUIVI MÉDICAL & VISITES PÉRIODIQUES
                Tout travailleur bénéficie d'un suivi régulier auprès de l'infirmerie du site : audiométrie, spirométrie, vision et bilan biologique annuel.
            """.trimIndent()
        ),
        HseDocument(
            id = "doc_env_pol",
            title = "Politique Environnementale",
            subtitle = "Engagements et objectifs environnementaux du groupe Holcim/CIMENCAM",
            category = "Environnement",
            format = "PDF Officiel",
            pagesCount = 7,
            referenceCode = "ENV-POL-2026",
            version = "v2.8 - 2026",
            author = "Direction Environnement",
            readingTimeMinutes = 4,
            summary = "Stratégie de réduction de l'empreinte environnementale, gestion de l'eau et valorisation des déchets.",
            content = """
                # 1. DÉCARBONATION & TRANSITION ÉNERGÉTIQUE
                CIMENCAM accélère sa trajectoire de neutralité carbone :
                - Remplacement progressif des combustibles fossiles par des biomasses locales (coques de cacao, balles de riz, huiles usagées recyclées).
                - Optimisation du facteur clinker par le développement de ciments éco-responsables à faible émission de CO2.

                # 2. PRÉSERVATION DE LA RESSOURCE EN EAU
                ✅ Traitement et réutilisation de 100% des eaux de procédé industriel en circuit fermé.
                ✅ Surveillance continue de la qualité des rejets pluviaux et des nappes phréatiques environnantes.
                ✅ Installation de séparateurs d'hydrocarbures sur toutes les plateformes de maintenance d'engins.

                # 3. GESTION ET VALORISATION DES DÉCHETS
                Tri sélectif rigoureux sur l'ensemble des sites (bacs identifiés : métaux, bois, plastiques, huiles, chiffons souillés). Traitement certifié des déchets dangereux par des filières agréées.

                # 4. RÉHABILITATION ET BIODIVERSITÉ DES CARRIÈRES
                Plan de réhabilitation continue des fronts de carrière exploités : remodelage paysager et reboisement avec des essences végétales locales adaptées.
            """.trimIndent()
        ),
        HseDocument(
            id = "doc_induction_livret",
            title = "Livret d'Accueil Sécurité Nouveaux Arrivants",
            subtitle = "Guide complet pour l'intégration sécurisée de tout visiteur ou nouvel employé",
            category = "Induction",
            format = "PDF Officiel",
            pagesCount = 10,
            referenceCode = "IND-LIV-001",
            version = "v4.2 - 2026",
            author = "Coordination Induction & Formation",
            readingTimeMinutes = 5,
            summary = "Guide indispensable contenant les consignes d'accès, les plans de circulation et les réflexes d'urgence.",
            content = """
                # 1. PROCÉDURE D'ACCÈS AUX SITES INDUSTRIELS
                Bienvenue sur les installations industrielles de CIMENCAM. Avant tout déplacement dans les zones opérationnelles :
                - Présentation obligatoire à la guérite principale pour vérification de l'identité et remise du pass visiteur.
                - Suivi obligatoire du module vidéo d'induction et signature du registre de consignes.
                - Port des EPI obligatoires remis par le service d'accueil.

                # 2. RÈGLES DE CIRCULATION PIÉTONNE ET VÉHICULE
                ⚠️ CONSIGNES VITALES :
                - Utilisez exclusivement les trottoirs et allées piétonnes délimitées en vert et jaune.
                - Ne marchez jamais dans les voies d'évolution des chargeuses, camions toupies et dumpers de carrière.
                - Établissez toujours un contact visuel avec les conducteurs d'engins avant de traverser.

                # 3. EN CAS D'ALARME OU D'ÉVACUATION
                1. Au déclenchement de la sirène d'alarme sonore continue :
                2. Interrompez immédiatement votre activité et coupez les machines si possible.
                3. Évacuez calmement en suivant le fléchage vert phosphorescent vers le Point de Rassemblement de votre secteur.
                4. Répondez à l'appel nominal du coordinateur d'évacuation et ne quittez jamais le point de rassemblement sans instruction formelle des pompiers du site.
            """.trimIndent()
        )
    )

    val sitePassagePlans = listOf(
        SitePassagePlan(
            id = "site_bonaberi",
            siteName = "Site de Bonabéri",
            title = "Plan de Passage — Site de Bonabéri",
            description = "Carte du site, accès portuaire/usine et points d'attention du site de Bonabéri",
            accessPoints = listOf("Portail Principal Entrée Nord", "Accès Quai Fluvial Déchargement Clinker", "Sortie Camions Ensachage"),
            musterPoints = listOf("Point de Rassemblement 1 (Parking Direction)", "Point de Rassemblement 2 (Zone Sud Broyage)"),
            infirmaryLocation = "Bâtiment Administratif RDC — Extension 100",
            emergencyNumber = "+237 233 39 12 00 (Ext. 100)",
            speedLimitKmH = 20,
            highRiskZones = listOf("Quai de déchargement clinker", "Broyage ciment et transporteurs à bandes", "Silos et trémies de stockage")
        ),
        SitePassagePlan(
            id = "site_figuil",
            siteName = "Site de Figuil",
            title = "Plan de Passage — Site de Figuil",
            description = "Carte du site, zones d'intervention, consignes de circulation et zones à risques du site de Figuil",
            accessPoints = listOf("Guérite d'accès Usine & Carrière", "Voie logistique expéditions sacs", "Accès réservé pompiers Ouest"),
            musterPoints = listOf("Point de Rassemblement Principal (Place d'Appel)", "Point Rassemblement 2 (Carrière Nord)"),
            infirmaryLocation = "Centre Médical Figuil — Bâtiment Santé poste 100",
            emergencyNumber = "+237 222 27 10 15 (Ext. 100)",
            speedLimitKmH = 20,
            highRiskZones = listOf("Four rotatif et échangeur thermique (haute température)", "Zone de tir de mines carrière", "Parc à matières premières et concasseur")
        ),
        SitePassagePlan(
            id = "site_nomayos",
            siteName = "Site de Nomayos",
            title = "Plan de Passage — Site de Nomayos",
            description = "Carte du site, flux logistiques et zones d'accès restreint du site de Nomayos",
            accessPoints = listOf("Entrée Principale Poids Lourds", "Poste de pesage et contrôle", "Accès administratif Ouest"),
            musterPoints = listOf("Point de Rassemblement A (Esplanade Accueil)", "Point de Rassemblement B (Parc logistique)"),
            infirmaryLocation = "Poste Médical Entrée — Poste 100",
            emergencyNumber = "+237 222 30 45 00 (Ext. 100)",
            speedLimitKmH = 20,
            highRiskZones = listOf("Ligne de palettisation automatisée", "Sous-station haute tension 90kV", "Zone de giration camions citernes")
        )
    )

    val emergencyFiches = listOf(
        EmergencyFicheMeta(
            id = "accident_corporel",
            title = "Consignes Accident Corporel",
            subtitle = "Que faire en cas d'accident corporel sur le site",
            iconType = "ambulance",
            stepsCount = 6,
            accentColorHex = 0xFFF97316
        ),
        EmergencyFicheMeta(
            id = "risques_chimiques",
            title = "Risques Chimiques",
            subtitle = "Procédures en cas de déversement ou contact chimique",
            iconType = "flask",
            stepsCount = 6,
            accentColorHex = 0xFF8B5CF6
        ),
        EmergencyFicheMeta(
            id = "risques_electriques",
            title = "Risques Électriques",
            subtitle = "Consignes d'urgence en cas d'incident électrique",
            iconType = "bolt",
            stepsCount = 6,
            accentColorHex = 0xFFF59E0B
        ),
        EmergencyFicheMeta(
            id = "evacuation_generale",
            title = "Plan d'Évacuation Général",
            subtitle = "Directives d'évacuation d'urgence de site",
            iconType = "exit",
            stepsCount = 6,
            accentColorHex = 0xFF06B6D4
        ),
        EmergencyFicheMeta(
            id = "incendie_evacuation",
            title = "Consignes Incendie & Évacuation",
            subtitle = "Procédure d'attaque du feu et alerte pompiers",
            iconType = "fire",
            stepsCount = 6,
            accentColorHex = 0xFFEF4444
        ),
        EmergencyFicheMeta(
            id = "premiers_secours",
            title = "Guide Premiers Secours",
            subtitle = "Gestes qui sauvent et prise en charge vitale",
            iconType = "plus",
            stepsCount = 6,
            accentColorHex = 0xFF10B981
        )
    )

    val videos = listOf(
        HseVideoMedia(
            id = "vid_induction_2026",
            title = "Induction Sécurité Nouveaux Arrivants & Prestataires",
            subtitle = "Module vidéo obligatoire avant tout accès aux zones de production et carrières",
            category = "Induction",
            durationText = "10:35",
            durationSeconds = 635,
            resolution = "1080p 60fps Full HD",
            presenter = "Mme. A. Manga — Coordinatrice Formation",
            site = "Tous sites CIMENCAM",
            viewsCount = 1420,
            isCertificateEligible = true,
            summary = "Vidéo d'accueil présentant les règles cardinales de sécurité, le comportement sur site industriel, les équipements de protection et les circuits d'évacuation d'urgence.",
            chapters = listOf(
                HseVideoChapter("01. Accueil & Valeurs Holcim", 0, "00:00", "Présentation de la culture Zéro Accident et du leadership partagé."),
                HseVideoChapter("02. Port systématique des EPI", 130, "02:10", "Casque avec jugulaire, lunettes, chaussures S3, gilet haute visibilité et protections auditives."),
                HseVideoChapter("03. Circulation piétons & engins", 310, "05:10", "Priorité piétonne, limitation à 20 km/h et contact visuel obligatoire."),
                HseVideoChapter("04. Consignation LOTO & permis de travail", 460, "07:40", "Cadenassage individuel et vérification d'absence de tension."),
                HseVideoChapter("05. Alerte & Évacuation", 570, "09:30", "Reconnaissance des sirènes, points de rassemblement et numéros d'urgence.")
            ),
            keyPoints = listOf(
                "Le droit de retrait sans sanction est garanti pour tout collaborateur en cas de danger grave.",
                "La vitesse est plafonnée à 20 km/h sur l'ensemble des voies usines et carrières.",
                "Interdiction formelle de circuler sous les charges suspendues en atelier d'ensachage.",
                "Ronde de surveillance obligatoire de 2h après tout travail par point chaud (permis de feu)."
            ),
            accentColorHex = 0xFF2563EB
        ),
        HseVideoMedia(
            id = "vid_loto_procedure",
            title = "Consignation Industrielle LOTO : Tolérance Zéro Énergie",
            subtitle = "Maîtrise complète des étapes de coupure, cadenassage et dissipation d'énergie",
            category = "Sécurité",
            durationText = "07:45",
            durationSeconds = 465,
            resolution = "4K UHD 60fps",
            presenter = "M. S. Nguema — Responsable Maintenance Électromécanique",
            site = "Site de Figuil & Bonabéri",
            viewsCount = 890,
            isCertificateEligible = true,
            summary = "Démonstration pas à pas de la consignation mécanique, électrique et pneumatique sur broyeur à ciment et transporteurs à bandes.",
            chapters = listOf(
                HseVideoChapter("01. Préparation & Information de l'équipe", 0, "00:00", "Analyse préalable des énergies résiduelles."),
                HseVideoChapter("02. Séparation & Coupure visible", 105, "01:45", "Manœuvre des sectionneurs et vannes principales."),
                HseVideoChapter("03. Cadenassage & Étiquetage nominal", 220, "03:40", "Pose du moraillon multiple et du cadenas personnel rouge."),
                HseVideoChapter("04. Dissipation des énergies résiduelles", 330, "05:30", "Purge d'air comprimé, calage mécanique des masses."),
                HseVideoChapter("05. Vérification d'Absence de Tension (VAT)", 400, "06:40", "Test au multimètre et tentative d'enclenchement à vide.")
            ),
            keyPoints = listOf(
                "Une personne = Un cadenas = Une clé personnelle (jamais de passe-partout partagé).",
                "Toujours tester le démarrage de la machine après consignation pour prouver l'isolement.",
                "La déconsignation ne peut être effectuée que par la personne ayant posé son cadenas."
            ),
            accentColorHex = 0xFFDC2626
        ),
        HseVideoMedia(
            id = "vid_premiers_secours",
            title = "Gestes d'Urgence & Utilisation du DAE sur Site",
            subtitle = "Réagir efficacement face à un arrêt cardiaque, un étouffement ou un traumatisme",
            category = "Santé",
            durationText = "06:15",
            durationSeconds = 375,
            resolution = "1080p Full HD",
            presenter = "Dr. C. Tagne — Médecin du Travail CIMENCAM",
            site = "Centre Médical Figuil / Infirmeries Usines",
            viewsCount = 1150,
            isCertificateEligible = true,
            summary = "Formation pratique aux gestes de secours vitaux : Protéger, Alerter, Secourir (PAS), massage cardiaque et défibrillateur automatisé externe.",
            chapters = listOf(
                HseVideoChapter("01. Protection de la zone d'accident", 0, "00:00", "Élimination des suraccidents (courant, gaz, feu)."),
                HseVideoChapter("02. Bilan rapide de la victime", 80, "01:20", "Conscience et respiration (L10 secondes)."),
                HseVideoChapter("03. Alerte des secours (Poste 100)", 160, "02:40", "Transmettre un message clair : Qui, Où, Quoi, État."),
                HseVideoChapter("04. Massage cardiaque (30:2)", 240, "04:00", "Rythme de 100 à 120 compressions par minute."),
                HseVideoChapter("05. Mise en place du Défibrillateur (DAE)", 310, "05:10", "Position des électrodes et écoute des instructions vocales.")
            ),
            keyPoints = listOf(
                "Chaque minute sans massage réduit les chances de survie de 10%.",
                "Ne jamais interrompre le massage cardiaque avant l'arrivée des pompiers ou du médecin.",
                "Des défibrillateurs sont disponibles à la guérite principale et à l'infirmerie de chaque site."
            ),
            accentColorHex = 0xFF10B981
        ),
        HseVideoMedia(
            id = "vid_env_decarbonation",
            title = "Transition Écologique & Valorisation des Biomasses",
            subtitle = "Comment CIMENCAM valorise les coques de cacao et réduit son empreinte CO2",
            category = "Environnement",
            durationText = "05:20",
            durationSeconds = 320,
            resolution = "4K UHD",
            presenter = "M. F. Kamga — Ingénieur Procédés & Économie Circulaire",
            site = "Usine de Nomayos & Figuil",
            viewsCount = 640,
            isCertificateEligible = false,
            summary = "Découvrez les innovations industrielles de CIMENCAM pour substituer les énergies fossiles par des sous-produits agricoles locaux et préserver les ressources naturelles.",
            chapters = listOf(
                HseVideoChapter("01. Enjeux climatiques de l'industrie cimentière", 0, "00:00", "Notre engagement vers la neutralité carbone 2050."),
                HseVideoChapter("02. La filière biomasse locale au Cameroun", 90, "01:30", "Collecte et stockage des coques de cacao et balles de riz."),
                HseVideoChapter("03. Co-incinération sécurisée en four rotatif", 190, "03:10", "Température de 1450°C assurant une combustion complète sans résidus."),
                HseVideoChapter("04. Suivi des émissions atmosphériques", 260, "04:20", "Analyseurs en continu et respect des normes environnementales.")
            ),
            keyPoints = listOf(
                "Plus de 35 000 tonnes de CO2 évitées chaque année grâce aux biomasses locales.",
                "100% des cendres issues de la combustion sont intégrées dans le clinker sans déchets.",
                "Soutien aux coopératives agricoles régionales pour l'approvisionnement durable."
            ),
            accentColorHex = 0xFF059669
        )
    )

    val images = listOf(
        HseImageMedia(
            id = "img_plan_bonaberi",
            title = "Plan Officiel de Circulation & Évacuation — Bonabéri",
            subtitle = "Carte technique haute définition avec zones de broyage, quais et points de rassemblement",
            category = "Plan de Passage",
            site = "Site de Bonabéri (Douala)",
            dateText = "Révision Mai 2026",
            author = "Bureau Technique Bonabéri",
            resolution = "4096 x 2896 (HD Vectorielle)",
            description = "Cartographie détaillée du site portuaire et de broyage de Bonabéri avec tracé des couloirs piétons sécurisés, sens unique de circulation camions toupies et balisage des équipements d'urgence.",
            themeColorHex = 0xFF1E3A8A,
            annotations = listOf(
                HseImageAnnotation("Entrée Guérite & Pesage", "Contrôle d'accès, remise des EPI et enregistrement des badges", 0.15f, 0.85f, false),
                HseImageAnnotation("Zone Quai Déchargement Clinker", "Zone portuaire avec port du gilet de sauvetage obligatoire aux abords", 0.78f, 0.20f, true),
                HseImageAnnotation("Point de Rassemblement 1 (PR-1)", "Esplanade principale face au bâtiment de direction", 0.40f, 0.70f, false),
                HseImageAnnotation("Poste Médical & Infirmerie RDC", "Localisation des secours d'urgence et du DAE", 0.35f, 0.60f, false),
                HseImageAnnotation("Atelier de Broyage Ciment", "Zone de bruit élevé (> 85 dB), port obligatoire du casque antibruit", 0.65f, 0.45f, true)
            ),
            technicalSpecs = listOf(
                "Vitesse maximale" to "20 km/h partout sur site",
                "Points de rassemblement" to "PR-1 (Direction) & PR-2 (Broyage Sud)",
                "Extincteurs et RIA" to "48 postes vérifiés mensuellement",
                "Poste de Secours" to "Bâtiment Administratif RDC (Poste 100)"
            )
        ),
        HseImageMedia(
            id = "img_affiche_12_regles",
            title = "Infographie des 12 Règles d'Or de Sécurité",
            subtitle = "Poster grand format affiché dans les ateliers, carrières et salles de contrôle",
            category = "Affiche Sécurité",
            site = "Tous sites CIMENCAM",
            dateText = "Campagne 2026",
            author = "Direction Communication",
            resolution = "3840 x 5430 (Affiche 4K)",
            description = "Support visuel officiel rappelant les 12 comportements qui sauvent des vies sur nos sites industriels, de l'isolation LOTO au port du harnais de sécurité.",
            themeColorHex = 0xFFDC2626,
            annotations = listOf(
                HseImageAnnotation("EPI Obligatoires", "Casque, lunettes, chaussures S3, gilet haute visibilité", 0.25f, 0.20f, false),
                HseImageAnnotation("Consignation LOTO", "Cadenas rouge nominatif, coupure visible et VAT", 0.75f, 0.20f, true),
                HseImageAnnotation("Travail en Hauteur", "Harnais avec double longe obligatoire dès 1,80 m", 0.25f, 0.50f, true),
                HseImageAnnotation("Circulation 20 km/h", "Priorité piétonne et port de la ceinture sur engins", 0.75f, 0.50f, false),
                HseImageAnnotation("Espaces Confinés", "Permis d'entrée, explosimètre et vigie externe", 0.25f, 0.80f, true),
                HseImageAnnotation("Droit de Retrait", "Arrêt immédiat de tout travail non maîtrisé", 0.75f, 0.80f, false)
            ),
            technicalSpecs = listOf(
                "Format d'affichage" to "A0 Plastifié haute résistance",
                "Norme de conformité" to "ISO 45001 & Standard Holcim",
                "Diffusion" to "Usines de Figuil, Nomayos et Bonabéri",
                "Langues" to "Français / Anglais"
            )
        ),
        HseImageMedia(
            id = "img_plan_figuil",
            title = "Plan Technique Général — Usine & Carrière de Figuil",
            subtitle = "Vue cartographique avec four rotatif, carrière de calcaire et zones thermiques",
            category = "Plan de Passage",
            site = "Site de Figuil (Nord)",
            dateText = "Révision Avril 2026",
            author = "Coordination Usine Figuil",
            resolution = "4096 x 2896 (HD)",
            description = "Plan détaillé de l'usine intégrée de Figuil, incluant les circuits logistiques de la carrière de calcaire, la zone du four à clinker haute température et les accès d'urgence pompiers.",
            themeColorHex = 0xFFEA580C,
            annotations = listOf(
                HseImageAnnotation("Front de Carrière Calcaire", "Zone de tir de mines sous protocole sonore strict", 0.80f, 0.25f, true),
                HseImageAnnotation("Four Rotatif & Tour Échangeur", "Zone haute température, port de la tenue ignifugée lors des rondes", 0.50f, 0.45f, true),
                HseImageAnnotation("Centre Médical Figuil", "Infirmerie équipée d'un médecin permanent et d'une ambulance", 0.20f, 0.75f, false),
                HseImageAnnotation("Point d'Appel Général", "Place d'appel en cas d'évacuation générale", 0.35f, 0.70f, false)
            ),
            technicalSpecs = listOf(
                "Superficie totale" to "145 Hectares sécurisés",
                "Ambulance de site" to "Disponible 24h/24 (Poste 100)",
                "Sirène d'alerte" to "Portée sonore de 3,5 km",
                "Pompiers d'usine" to "Équipe d'intervention permanente"
            )
        ),
        HseImageMedia(
            id = "img_affiche_epi",
            title = "Guide Visuel des Équipements de Protection (EPI)",
            subtitle = "Détail des normes techniques et critères de conformité des équipements de protection",
            category = "Affiche Sécurité",
            site = "Tous sites CIMENCAM",
            dateText = "Édition 2026",
            author = "Service Prévention des Risques",
            resolution = "3840 x 4800 (HD)",
            description = "Fiche pédagogique illustrant le bon port, l'inspection avant utilisation et les critères de mise au rebut des équipements de protection individuelle.",
            themeColorHex = 0xFF10B981,
            annotations = listOf(
                HseImageAnnotation("Casque de Sécurité", "Jugulaire clipsée, durée de vie maximale 3 ans", 0.50f, 0.15f, false),
                HseImageAnnotation("Lunettes de Protection", "Norme EN 166 avec protection latérale anti-poussière", 0.50f, 0.32f, false),
                HseImageAnnotation("Masque FFP2 / FFP3", "Remplacement après chaque quart en zone broyage", 0.50f, 0.48f, false),
                HseImageAnnotation("Gants Anti-Coupure", "Niveau 5 pour manipulation de tôles et ferrailles", 0.30f, 0.65f, false),
                HseImageAnnotation("Chaussures S3", "Embout acier 200J et semelle anti-perforation inox", 0.50f, 0.85f, false)
            ),
            technicalSpecs = listOf(
                "Dotation" to "Fournie gratuitement par l'entreprise",
                "Contrôle" to "Vérification quotidienne par les chefs de quart",
                "Remplacement" to "Immédiat au magasin général en cas d'usure",
                "Marquage" to "Norme CE / ISO conforme"
            )
        )
    )
}

