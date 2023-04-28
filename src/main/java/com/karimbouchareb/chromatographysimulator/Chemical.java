//package com.karimbouchareb.chromatographysimulator;
//
//import java.util.HashMap;
//
//// TODO: 4/3/2023 This enum class should eventually be a member class of ChromatographySimulator
//// TODO: 4/9/2023 ADD CAS-ID FIELD TO ALL OF THESE
//public enum Chemical {
//	ACETONAPHTHONE("941-98-0", "1-Acetonaphthone", 1.517, 1.436, 0, 0.557, 6.649, 2),
//	BROMODODECANE("143-15-7", "1-Bromododecane", 0.332, 0.374, 0, 0.3, 7.287, 0),
//	BROMONAPHTHALENE("90-11-9", "1-Bromonaphthalene", 1.594, 1.033, 0, 0.151, 6.558, 2),
//	CHLORONAPHTHALENE("90-13-1", "1-Chloronaphthalene", 1.419, 0.964, 0, 0.131, 6.188, 2),
//	HEXYNE("693-02-7", "1-Hexyne", 0.167, 0.275, 0.091, 0.118, 2.541, 0),
//	HYDROXYANTHRAQUINONE("129-43-1", "1-Hydroxyanthraquinone", 1.502, 1.489, 0.049, 0.542, 9.077, 2),
//	METHYLNAPHTHALENE_1("90-12-0", "1-Methylnaphthalene", 1.332, 0.916, 0, 0.196, 5.699, 2),
//	NAPTHOL("90-15-3", "1-Naphthol", 1.407, 1.101, 0.758, 0.332, 6.192, 2),
//	NITRONAPHTHALENE("86-57-7", "1-Nitronaphthalene", 1.392, 1.482, 0, 0.288, 6.842, 2),
//	NITROPROPANE_1("108-03-2", "1-Nitropropane", 0.243, 0.926, 0.046, 0.267, 2.882, 0),
//	NITROPYRENE("5522-43-0", "1-Nitropyrene", 2.789, 2.063, 0, 0.336, 10.474, 2),
//	PHENYLETHANOL_1("98-85-1", "1-Phenylethanol", 0.782, 0.725, 0.424, 0.66, 4.473, 1),
//	PHENYL_PROPANOL("93-54-9", "1-Phenyl-1-propanol", 0.81, 0.866, 0.395, 0.665, 5.159, 1),
//	PHENYL_ISOPROPANOL("698-87-3", "1-Phenyl-2-propanol", 0.787, 0.782, 0.316, 0.7, 4.835, 1),
//	DICHLOROBENZENE_1_2("95-50-1", "1,2-Dichlorobenzene", 0.872, 0.775, 0, 0.04, 4.507, 1),
//	DIMETHYLBENZENE_1_2("95-47-6", "1,2-Dimethylbenzene", 0.663, 0.547, 0, 0.178, 3.948, 1),
//	TRICHLOROBENZENE("120-82-1", "1,2,4-Trichlorobenzene", 1.022, 0.746, 0, 0.024, 5.198, 1),
//	TETRACHLOROBENZENE("95-94-3", "1,2,4,5-Tetrachlorobenzene", 1.022, 0.695, 0, 0.02, 5.957, 1),
//	DIBENZOANTHRACENE("53-70-3", "1,2,5,6-Dibenzoanthracene", 3.837, 2.286, 0, 0.54, 12.53, 4),
//	DIBROMOBENZENE("108-36-1", "1,3-Dibromobenzene", 1.198, 0.798, 0, 0.074, 5.218, 1),
//	DICHLOROBENZENE_1_3("541-73-1", "1,3-Dichlorobenzene", 0.852, 0.692, 0, 0.004, 4.421, 1),
//	DIHYDROXYBENZENE("108-46-3", "1,3-Dihydroxybenzene", 0.973, 0.963, 1.286, 0.498, 4.877, 1),
//	DIMETHYLBENZENE_1_3("108-38-3", "1,3-Dimethylbenzene", 0.625, 0.504, 0, 0.183, 3.845, 1),
//	DINITROBENZENE("99-65-0", "1,3-Dinitrobenzene", 0.985, 1.715, 0, 0.424, 5.971, 1),
//	TRIETHYLBENZENE("102-25-0", "1,3,5-Triethylbenzene", 0.67, 0.5, 0, 0.19, 5.511, 1),
//	DIMETHYLBENZENE_1_4("106-42-3", "1,4-Dimethylbenzene", 0.615, 0.494, 0, 0.165, 3.852, 1),
//	DICHLOROTETRAMETHYLDISILOXANE("2401-73-2", "1,3-Dichloro-1,1,3,3-tetramethyldisiloxane", -0.066, -0.012, 0, 0, 4.168, 0),
//	DICHLOROHEXAMETHYLTRISILOXANE("3582-71-6", "1,5-Dichloro-1,1,3,3,5,5-hexamethyltrisiloxane", -0.234, 0.313, 0, 0, 4.539, 0),
//	DODECANEDIOL_1_12("5675-51-4", "1,12-Dodecanediol", 0.456, 0.807, 0.817, 1.217, 7.531, 0),
//	NAPHTHYLETHANOL("773-99-9", "2-(1-Naphthyl)ethanol", 1.583, 1.247, 0.426, 0.74, 6.984, 2),
//	ACETONAPHTHONE_2("93-08-3", "2-Acetonaphthone", 1.481, 1.478, 0, 0.6, 6.765, 2),
//	CHLOROACETAMIDE("79-07-2", "2-Chloroacetamide", 0.617, 1.211, 0.408, 0.678, 3.292, 0),
//	CHLOROANILINE("95-51-2", "2-Chloroaniline", 1.026, 0.991, 0.243, 0.315, 4.685, 1),
//	CHLOROPHENOL("95-57-8", "2-Chlorophenol", 0.879, 0.66, 0.535, 0.342, 4.124, 1),
//	ETHOXYETHANOL("110-80-5", "2-Ethoxyethanol", 0.237, 0.554, 0.324, 0.785, 2.793, 0),
//	HEPTANONE("110-43-0", "2-Heptanone", 0.123, 0.657, 0, 0.487, 3.789, 0),
//	HEXANONE("591-78-6", "2-Hexanone", 0.136, 0.668, 0, 0.502, 3.273, 0),
//	METHOXYCINNAMALDEHYDE("1504-74-1", "2-Methoxycinnamaldehyde", 1.143, 1.517, 0, 0.589, 6.322, 1),
//	METHOXYETHANOL("109-86-4", "2-Methoxyethanol", 0.268, 0.591, 0.362, 0.84, 2.338, 0),
//	METHOXYNAPHTHALENE("93-04-9", "2-Methoxynaphthalene", 1.476, 1.161, 0, 0.354, 6.232, 2),
//	METHYLBUTANOL_2_2("75-85-4", "2-Methyl-2-butanol", 0.193, 0.388, 0.247, 0.657, 2.538, 0),
//	METHYLANILINE("95-53-4", "2-Methylaniline", 0.771, 1.022, 0.208, 0.443, 4.321, 1),
//	METHYLBENZOTHIAZOLE("120-75-2", "2-Methylbenzothiazole", 1.075, 0.953, 0, 0.558, 5.78, 1),
//	METHYLBUTANOL_2("137-32-6", "2-Methylbutanol", 0.217, 0.429, 0.336, 0.52, 2.981, 0),
//	METHYLNAPHTHALENE_2("91-57-6", "2-Methylnaphthalene", 1.197, 0.921, 0, 0.19, 5.778, 2),
//	METHYLPHENOL_2("95-48-7", "2-Methylphenol", 0.772, 0.748, 0.607, 0.355, 4.281, 1),
//	METHYLPROPANOL("78-83-1", "2-Methylpropanol", 0.206, 0.386, 0.362, 0.508, 2.401, 0),
//	NAPHTHOL("135-19-3", "2-Naphthol", 1.467, 1.187, 0.773, 0.347, 6.144, 2),
//	NITROANILINE_2("88-74-4", "2-Nitroaniline", 1.215, 1.467, 0.346, 0.353, 5.771, 1),
//	NITROPHENOL("88-75-5", "2-Nitrophenol", 0.942, 1.107, 0.033, 0.374, 4.731, 1),
//	NITROPROPANE_2("79-46-9", "2-Nitropropane", 0.216, 0.898, 0.01, 0.312, 2.707, 0),
//	NITROTOLUENE_2("88-72-2", "2-Nitrotoluene", 0.866, 1.115, 0, 0.269, 4.989, 1),
//	NONANONE("821-55-6", "2-Nonanone", 0.113, 0.676, 0, 0.467, 4.764, 0),
//	OCTANONE("111-13-7", "2-Octanone", 0.109, 0.661, 0, 0.509, 4.277, 0),
//	PENTANONE("107-87-9", "2-Pentanone", 0.143, 0.681, 0, 0.485, 2.731, 0),
//	PHENYLACETAMIDE("103-81-1", "2-Phenylacetamide", 1.294, 1.729, 0.345, 0.944, 5.376, 1),
//	PHENYLETHANOL_2("60-12-8", "2-Phenylethanol", 0.808, 0.821, 0.409, 0.629, 4.698, 1),
//	PHENYLPROPANOL("1123-85-9", "2-Phenyl-1-propanol", 0.81, 0.866, 0.395, 0.665, 5.159, 1),
//	UNDECANONE("112-12-9", "2-Undecanone", 0.105, 0.679, 0, 0.494, 5.721, 0),
//	DIMETHYLPHENOL_2_3("526-75-0", "2,3-Dimethylphenol", 0.851, 0.8, 0.564, 0.402, 4.862, 1),
//	DIMETHYLANILINE("95-68-1", "2,4-Dimethylaniline", 1.003, 0.726, 0.373, 0.65, 5.003, 1),
//	DINITROANILINE("97-02-9", "2,4-Dinitroaniline", 1.496, 2.351, 0.284, 0.507, 7.681, 1),
//	DICHLORONITROANILINE("99-30-9", "2,6-Dichloro-4-nitroaniline", 1.078, 1.247, 0.492, 0.352, 7.443, 1),
//	DIMETHYLPHENOL_2_6 ("576-26-1", "2,6-Dimethylphenol", 0.752, 0.774, 0.413, 0.406, 4.794, 1),
//	METHYLANILINE_3("108-44-1", "3-Methylaniline", 0.779, 1.058, 0.195, 0.457, 4.404, 1),
//	METHYLIBUTANOL_3("123-51-3", "3-Methylbutanol", 0.198, 0.423, 0.35, 0.513, 2.968, 0),
//	METHYLPHENOL_3("108-39-4", "3-Methylphenol", 0.776, 0.771, 0.695, 0.339, 4.327, 1),
//	NITROANILINE_3("99-09-2", "3-Nitroaniline", 1.234, 1.535, 0.49, 0.429, 6.034, 1),
//	NITROTOLUENE_3("99-08-1", "3-Nitrotoluene", 0.874, 1.121, 0, 0.252, 5.241, 1),
//	PHENYLPROPANOL_3("122-97-4", "3-Phenylpropan-1-ol", 0.81, 0.866, 0.395, 0.665, 5.159, 1),
//	DICHLOROBENZIDINE_33("91-94-1", "3,3’-Dichlorobenzidine", 1.737, 1.921, 0.752, 0.436, 10.025, 2),
//	DICHLOANILINE_34("95-76-1", "3,4-Dichloroaniline", 1.218, 1.21, 0.421, 0.247, 5.973, 1),
//	DIMETHYLPHENOL_35("108-68-9", "3,5-Dimethylphenol", 0.786, 0.799, 0.662, 0.337, 4.759, 1),
//	ACETYLBIOPHENYL_4("92-91-1", "4-Acetylbiphenyl", 1.505, 1.526, 0, 0.611, 7.661, 2),
//	BROMOACETANILIDE_4("103-88-8", "4-Bromoacetanilide", 1.065, 1.537, 0.806, 0.451, 6.89, 1),
//	CHLORONITROANILINE_4("89-63-4", "4-Chloro-2-nitroaniline", 0.985, 1.425, 0.453, 0.184, 6.781, 1),
//	CHLOROANILINE_4("106-47-8", "4-Chloroaniline", 0.979, 1.11, 0.341, 0.322, 4.977, 1),
//	CHLOROPHENOL_4("106-48-9", "4-Chlorophenol", 1.032, 0.81, 0.873, 0.202, 4.795, 1),
//	CYANOPHENOL_4("767-00-0", "4-Cyanophenol", 1.099, 1.261, 0.824, 0.385, 5.486, 1),
//	DIMETHYLAMINOAZOBENZENE_4("60-11-7", "4-Dimethylaminoazobenzene", 2.057, 1.748, 0, 1.532, 8.516, 1),
//	FLUOROANILINE_4("371-40-4", "4-Fluoroaniline", 0.723, 0.958, 0.331, 0.41, 4.063, 1),
//	HYDROXYBENZALDEHYDE("123-08-0", "4-Hydroxybenzaldehyde", 1.156, 1.263, 0.913, 0.467, 5.243, 1),
//	HYDROXYBENZYL_ALCOHOL("623-05-2", "4-Hydroxybenzyl alcohol", 1.24, 1.285, 0.989, 0.764, 5.119, 1),
//	METHOXY_NITROANILINE("96-96-8", "4-Methoxy-2-nitroaniline", 1.294, 1.577, 0.314, 0.418, 6.791, 1),
//	METHOXYBENZYL_ALCOHOL("105-13-5", "4-Methoxybenzyl alcohol", 0.888, 0.968, 0.544, 0.765, 5.326, 1),
//	METHYLPHENOL_4("106-44-5", "4-Methylphenol", 0.828, 0.791, 0.664, 0.364, 4.314, 1),
//	NITROANILINE_4("100-01-6", "4-Nitroaniline", 1.233, 1.813, 0.591, 0.346, 6.383, 1),
//	NITROBENZYL_ALCOHOL("619-73-8", "4-Nitrobenzyl alcohol", 1.008, 1.356, 0.51, 0.584, 6.347, 1),
//	NITROTOLUENE_4("99-99-0", "4-Nitrotoluene", 0.858, 1.153, 0, 0.268, 5.282, 1),
//	PHENYLPHENOL("92-69-3", "4-Phenylphenol", 1.537, 1.187, 0.794, 0.45, 7.058, 2),
//	DIBROMOBIPHENYL("92-86-4", "4,4’-Dibromobiphenyl", 2.126, 1.173, 0, 0.247, 8.674, 2),
//	METHYLENEDIANILINE("101-77-9", "4,4’-Methylenedianiline", 2.013, 1.696, 0.662, 0.861, 8.522, 2),
//	HYDROXYQUINOLINE("148-24-3", "8-Hydroxyquinoline", 1.037, 1.082, 0.196, 0.388, 5.967, 2),
//	ACENAPHTHENE("83-32-9", "Acenaphthene", 1.44, 0.941, 0, 0.219, 6.727, 2),
//	ACENAPHTHYLENE("208-96-8", "Acenaphthylene", 1.566, 1.15, 0, 0.201, 6.382, 2),
//	ACETAMIDE("60-35-5", "Acetamide", 0.268, 1.076, 0.718, 0.69, 2.722, 0),
//	ACETOACETANILIDE("102-01-2", "Acetoacetanilide", 2.055, 0.187, 0.602, 1.65, 4.805, 1),
//	ACETOPHENONE("98-86-2", "Acetophenone", 0.806, 1.057, 0, 0.496, 4.488, 1),
//	ALPHA_ISOMETHYL_IONONE("127-51-5", "alpha-Isomethyl ionone", 0.721, 0.843, 0, 0.812, 6.485, 0),
//	ALPHA_PINENE("80-56-8", "alpha-Pinene", 0.441, 0.191, 0, 0.226, 4.345, 0),
//	AMYL_CINNAMAL("122-40-7", "Amyl cinnamal", 1.212, 1.046, 0, 0.74, 7.132, 1),
//	ANDROSTERONE("53-41-8", "Androsterone", 1.327, 1.699, 0.394, 1.592, 10.745, 0),
//	ANILINE("62-53-3", "Aniline", 0.955, 1.021, 0.239, 0.424, 3.944, 1),
//	ANISALDEHYDE("123-11-5", "Anisaldehyde", 0.92, 1.35, 0, 0.47, 5.304, 1),
//	ANISOLE("100-66-3", "Anisole", 0.712, 0.761, 0, 0.313, 3.81, 1),
//	ANTHRACENE("120-12-7", "Anthracene", 1.995, 1.309, 0, 0.259, 7.723, 2),
//	ANTHRAQUINONE("84-65-1", "Anthraquinone", 1.735, 1.504, 0, 0.563, 8.572, 2),
//	AZULENE("275-51-4", "Azulene", 1.466, 1.085, 0, 0.21, 5.736, 0),
//	BENZ_A_ANTHRACENE("56-55-3", "Benz[a]anthracene", 2.735, 1.678, 0, 0.368, 10.124, 4),
//	BENZALDEHYDE("100-52-7", "Benzaldehyde", 0.813, 1.027, 0, 0.395, 4.003, 1),
//	BENZAMIDE("55-21-0", "Benzamide", 1.268, 1.383, 0.637, 0.659, 5.244, 1),
//	BENZENE("71-43-2", "Benzene", 0.608, 0.506, 0, 0.144, 2.784, 1),
//	BENZENESULFONAMIDE("98-10-2", "Benzenesulfonamide", 1.14, 1.871, 0.658, 0.671, 6.055, 1),
//	BENZIDINE("92-87-5", "Benzidine", 1.116, 1.567, 0.228, 1.018, 9.166, 2),
//	BENZODIOXANE("493-09-4", "Benzodioxane", 0.884, 1.064, 0, 0.304, 4.952, 1),
//	BENZO_A_PYRENE("50-32-8", "Benzo[a]pyrene", 3.033, 1.846, 0, 0.418, 11.54, 5),
//	BENZO_E_PYRENE("192-97-2", "Benzo[e]pyrene", 3.0, 1.89, 0, 0.41, 11.436, 5),
//	BENZONITRILE("100-47-0", "Benzonitrile", 0.742, 1.128, 0, 0.332, 4.055, 1),
//	BENZOPHENONE("119-61-9", "Benzophenone", 1.21, 1.295, 0, 0.58, 7.059, 2),
//	BENZOTHIAZOLE("95-16-9", "Benzothiazole", 1.08, 1.109, 0, 0.488, 5.333, 1),
//	BENZYL_2_ETHYLHEXYL_PHTHALATE("18750-05-5", "Benzyl 2-ethylhexyl phthalate", 1.405, 1.303, 0, 1.341, 10.823, 1),
//	BENZYL_ALCOHOL("100-51-6", "Benzyl alcohol", 0.804, 0.872, 0.409, 0.557, 4.248, 1),
//	BENZYL_CINNAMATE("103-41-3", "Benzyl cinnamate", 1.206, 1.509, 0, 0.622, 8.997, 2),
//	BENZYL_SALICYLATE("118-58-1", "Benzyl salicylate", 1.317, 1.263, 0.041, 0.438, 8.073, 1),
//	BETA_PINENE("127-91-3", "Beta-Pinene", 0.525, 0.233, 0, 0.208, 4.584, 1),
//	BIPHENYL("92-52-4", "Biphenyl", 1.342, 0.987, 0, 0.284, 6.044, 2),
//	BORNEOL("507-70-0", "Borneol", 0.671, 0.638, 0.201, 0.634, 5.135, 0),
//	BROMOBENZENE("108-86-1", "Bromobenzene", 0.882, 0.729, 0, 0.092, 4.038, 1),
//	BUTAN_1_OL("71-36-3", "Butan-1-ol", 0.219, 0.446, 0.336, 0.48, 2.577, 0),
//	N_BUTYLBENZENE("104-51-8", "n-Butylbenzene", 0.595, 0.484, 0, 0.139, 4.75, 1),
//	BUTYL_ACETATE("123-86-4", "Butyl acetate", 0.079, 0.565, 0, 0.476, 3.417, 0),
//	BUTYL_BENZOATE("136-60-7", "Butyl benzoate", 0.668, 0.877, 0, 0.389, 5.954, 1),
//	BUTYL_BENZYL_PHTHALATE("85-68-7", "Butyl benzyl phthalate", 1.296, 1.72, 0, 1.007, 9.803, 1),
//	BUTYL_CYCLOHEXYL_PHTHALATE("84-64-0", "Butyl cyclohexyl phthalate", 1.083, 1.423, 0, 0.911, 9.6, 0),
//	BUTYL_DECYL_PHTHALATE("89-19-0", "Butyl decyl phthalate", 0.742, 1.354, 0, 1.689, 8.397, 0),
//	BUTYL_2_ETHYLHEXYL_PHTHALATE("85-69-8", "Butyl 2-ethylhexyl phthalate", 0.722, 1.144, 0, 1.045, 11.287, 0),
//	BUTYL_OCTYL_PHTHALATE("84-78-6", "Butyl octyl phthalate", 0.751, 1.593, 0, 0.938, 8.32, 0),
//	BUTYL_OLEATE("142-77-8", "Butyl oleate", 0.024, 0.628, 0, 0.582, 10.855, 0),
//	BUTYL_STEARATE("123-95-5", "Butyl stearate", 0.051, 0.52, 0, 0.645, 11.001, 0),
//	CAMPHOR("507-70-0", "Camphor", 0.573, 0.846, 0, 0.664, 5.031, 0),
//	CARBAZOLE("86-74-8", "Carbazole", 2.271, 1.633, 0.393, 0.157, 7.39, 0),
//	CARVONE("99-49-0", "Carvone", 0.638, 0.928, 0, 0.611, 5.406, 0),
//	CHLOROBENZENE("108-90-7", "Chlorobenzene", 0.718, 0.656, 0, 0.058, 3.622, 1),
//	CHOLESTANE("481-21-0", "Cholestane", 1.186, 0.391, 0, 0, 13.004, 0),
//	CHOLESTEROL("57-88-5", "Cholesterol", 1.353, 1.097, 0.212, 0.558, 13.389, 0),
//	CHOLESTERYL_ACETATE("604-35-3", "Cholesteryl acetate", 1.234, 0.977, 0, 0.334, 14.276, 0),
//	CHRYSNE("218-01-9", "Chrysene", 2.593, 1.66, 0, 0.294, 10.142, 4),
//	CINNAMYL_ALCOHOL("104-54-1", "Cinnamyl alcohol", 1.1, 0.97, 0.469, 0.598, 5.465, 1),
//	CITRONELLAL("106-23-0", "Citronellal", 0.287, 0.674, 0, 0.756, 5.076, 0),
//	COUMARIN("91-64-5", "Coumarin", 1.426, 1.649, 0, 0.525, 5.966, 1),
//	CYCLOHEXANOL("108-93-0", "Cyclohexanol", 0.474, 0.638, 0.246, 0.583, 3.722, 0),
//	CYCLOHEXANONE("108-94-1", "Cyclohexanone", 0.403, 0.887, 0, 0.531, 3.771, 0),
//	CYCLOHEXANONE_OXIME("100-64-1", "Cyclohexanone oxime", 0.73, 0.677, 0.45, 0.596, 4.442, 0),
//	DECAN_1_OL("112-30-1", "Decan-1-ol", 0.26, 0.444, 0.318, 0.549, 5.611, 0),
//	N_DECANE("124-18-5", "n-Decane", 0, 0, 0, 0, 4.7, 0),
//	DI_2_METHOXYETHYL_PHTHALATE("117-82-8", "Di-(2-Methoxyethyl) phthalate", 0.788, 1.742, 0, 1.507, 8.35, 0),
//	DI_2_ETHYLHEXYL_PHTHALATE("117-81-7", "Di-2-Ethylhexyl phthalate", 0.693, 1.157, 0, 1.401, 11.255, 0),
//	DI_N_BUTYL_PHTHALATE("84-74-2", "Di-n-Butyl phthalate", 0.694, 1.321, 0, 0.93, 8.508, 0),
//	DI_N_BUTYL_SUCCINATE("141-03-7", "Di-n-Butyl succinate", 0.091, 0.94, 0, 0.963, 6.867, 0),
//	DI_N_OCTYL_PHTHALATE("117-84-0", "Di-n-Octyl phthalate", 0.662, 1.291, 0, 1.13, 11.957, 0),
//	DI_2_ETHOXYETHYL_PHTHALATE("117-81-7", "Di(2-Ethoxyethyl) phthalate", 0.619, 1.559, 0, 1.495, 9.043, 0),
//	DI_2_N_BUTOXYETHYL_PHTHALATE("117-83-9", "Di(2-n-Butoxyethyl) phthalate", 1.084, 1.505, 0, 1.458, 10.546, 0),
//	DIBENZOFURAN("132-64-9", "Dibenzofuran", 1.633, 1.112, 0, 0.105, 6.619, 2),
//	DIBENZYL_ETHER("103-50-4", "Dibenzyl ether", 1.21, 1.1, 0, 0.727, 7.17, 2),
//	DIBENZYLAMINE("103-49-1", "Dibenzylamine", 1.418, 1.005, 0.068, 0.99, 7.529, 2),
//	DICYCLOHEXYL_PHTHALATE("84-61-7", "Dicyclohexyl phthalate", 1.464, 1.516, 0, 1.064, 10.722, 0),
//	DICYCLOHEXYLAMINE("101-83-7", "Dicyclohexylamine", 0.576, 0.419, 0, 0.667, 6.665, 0),
//	DICYLCOHEXYL_ADIPATE("849-99-0", "Dicyclohexyl adipate", 0.649, 1.275, 0, 1.073, 10.052, 0),
//	DIETHYL_ADIPATE("141-28-6", "Diethyl adipate", 0.085, 1.063, 0, 0.835, 5.891, 0),
//	DIETHYL_DIETHYLMALONATE("77-25-8", "Diethyl diethylmalonate", 0.01, 0.729, 0, 0.867, 5.645, 0),
//	DIETHYL_PHTHALATE("84-66-2", "Diethyl phthalate", 0.725, 1.393, 0, 0.887, 6.68, 0),
//	DIETHYL_SEBECATE("110-40-7", "Diethyl sebacate", 0.043, 1.058, 0, 0.981, 7.871, 0),
//	DIHYDROCHOLESTEROL("80-97-7", "Dihydrocholesterol", 1.333, 1.046, 0.207, 0.638, 13.525, 0),
//	DIISOBUTYL_PHTHALATE("84-69-5", "Diisobutyl phthalate", 0.672, 1.246, 0, 0.939, 8.094, 0),
//	DIMETHYL_PHTHALATE("131-11-3", "Dimethyl phthalate", 0.795, 1.502, 0, 0.798, 5.965, 0),
//	DIOXANE("123-91-1", "Dioxane", 0.329, 0.739, 0, 0.592, 2.833, 0),
//	DIPHENYL_ETHER("101-84-8", "Diphenyl ether", 1.168, 0.936, 0, 0.329, 6.107, 2),
//	DIPHENYLAMINE("122-39-4", "Diphenylamine", 1.432, 1.223, 0.186, 0.461, 6.892, 2),
//	N_DODECANE("112-40-3", "n-Dodecane", 0, 0, 0, 0, 5.685, 0),
//	DODECAN_1_OL("112-53-8", "Dodecan-1-ol", 0.19, 0.433, 0.346, 0.53, 6.655, 0),
//	ETHYL_BENZOATE("93-89-0", "Ethyl benzoate", 0.694, 0.897, 0, 0.451, 5.022, 1),
//	ETHYL_2_METHYLBUTYRATE("7452-79-1", "Ethyl 2-methylbutyrate", 0.319, 0.427, 0, 0.45, 3.699, 0),
//	ETHYL_OLEATE("111-62-6", "Ethyl oleate", 0.144, 0.582, 0, 0.625, 9.978, 0),
//	ETHYL_PROPIONATE("105-37-3", "Ethyl propionate", 0.092, 0.533, 0, 0.45, 2.889, 0),
//	ETHYLBENZENE("100-41-4", "Ethylbenzene", 0.613, 0.509, 0, 0.147, 3.8, 1),
//	EUGENOL("97-53-0", "Eugenol", 1.076, 0.82, 0.398, 0.572, 5.765, 1),
//	FLUORANTHENE("206-44-0", "Fluoranthene", 2.402, 1.51, 0, 0.292, 8.719, 3),
//	FLUORENE("86-73-7", "Fluorene", 1.659, 1.103, 0, 0.255, 6.952, 2),
//	GERANIAL("141-27-5", "Geranial", 0.612, 0.938, 0, 0.659, 5.463, 0),
//	GERANIOL("106-24-1", "Geraniol", 0.493, 0.635, 0.282, 0.573, 5.416, 0),
//	HEPTANAL("111-71-7", "Heptanal", 0.14, 0.643, 0, 0.435, 3.855, 0),
//	N_HEPTANE("142-82-5", "n-Heptane", 0, 0, 0, 0, 3.173, 0),
//	HEPTAN_1_OL("111-70-6", "Heptan-1-ol", 0.202, 0.45, 0.335, 0.531, 4.132, 0),
//	HEXACHLOROBENZENE("118-74-1", "Hexachlorobenzene", 1.53, 0.819, 0, 0.126, 7.73, 1),
//	N_HEXADECANE("544-76-3", "n-Hexadecane", 0, 0, 0, 0, 7.714, 0),
//	HEXAMETHYLDISILOXANE("107-46-0", "Hexamethyldisiloxane", -0.27, -0.187, 0, 0.299, 3.115, 0),
//	HEXANAL("66-25-1", "Hexanal", 0.146, 0.639, 0, 0.45, 3.35, 0),
//	N_HEXANE("110-54-3", "n-Hexane", 0, 0, 0, 0, 2.668, 0),
//	HEXAN_1_OL("111-27-3", "Hexan-1-ol", 0.21, 0.432, 0.35, 0.535, 3.646, 0),
//	HYDROXYCITRONELLAL("107-75-5", "Hydroxycitronellal", 0.54, 0.562, 0.68, 1.08, 5.496, 0),
//	LILIAL("80-54-6", "Lilial", 0.775, 0.994, 0, 0.599, 6.659, 1),
//	LIMONENE("138-86-3", "Limonene", 0.497, 0.338, 0, 0.164, 4.689, 0),
//	LINALOOL("78-70-6", "Linalool", 0.325, 0.524, 0.199, 0.693, 4.783, 0),
//	METHYL_ABIETATE("127-25-3", "Methyl abietate", 1.164, 1.091, 0, 1.108, 10.268, 0),
//	METHYL_BENZOATE("93-58-3", "Methyl benzoate", 0.738, 0.916, 0, 0.441, 4.681, 1),
//	METHYL_CYCLOHEXANE("108-87-2", "Methyl Cyclohexane", 0.244, 0.118, 0, 0, 3.318, 0),
//	METHYL_DECANOATE("110-42-9", "Methyl decanoate", 0.057, 0.558, 0, 0.489, 5.872, 0),
//	METHYL_EUGENOL("93-15-2", "Methyl eugenol", 0.939, 0.991, 0, 0.827, 5.977, 1),
//	METHYL_HEXANOATE("106-70-7", "Methyl hexanoate", 0.084, 0.566, 0, 0.47, 3.967, 0),
//	METHYL_NONANOATE("1731-84-6", "Methyl nonanoate", 0.054, 0.557, 0, 0.449, 5.421, 0),
//	METHYL_OCTANOATE("111-11-5", "Methyl octanoate", 0.069, 0.557, 0, 0.448, 4.955, 0),
//	METHYL_PHENYL_ETHER("100-66-3", "Methyl phenyl ether", 0.71, 0.77, 0, 0.31, 3.808, 1),
//	METHYLTRIETHOXYSILANE("2031-67-6", "Methyltriethoxysilane", -0.23, 0.249, 0, 0.751, 3.869, 0),
//	N_METHYLACETAMIDE("79-16-3", "N-Methylacetamide", 0.263, 1.293, 0.157, 0.663, 3.133, 0),
//	N_METHYLANILINE("100-61-8", "N-Methylaniline", 0.95, 0.9, 0.17, 0.48, 4.478, 1),
//	NAPHTHALENE("91-20-3", "Naphthalene", 1.241, 0.921, 0, 0.188, 5.144, 2),
//	NERAL("106-26-3", "Neral", 0.589, 0.901, 0, 0.65, 5.391, 0),
//	NICOTINAMIDE("98-92-0", "Nicotinamide", 1.227, 1.804, 0.43, 0.779, 5.342, 1),
//	NICOTINE("23950-04-1", "Nicotine", 0.882, 0.959, 0, 1.082, 5.92, 1),
//	NITROBENZENE("98-95-3", "Nitrobenzene", 0.846, 1.143, 0, 0.268, 4.53, 1),
//	N_N_DIETHYL_4_NITROANILINE("2216-15-1", "N,N-Diethyl-4-nitroaniline", 1.316, 1.722, 0, 0.621, 7.965, 1),
//	N_N_DIETHYLCARBANILIDE("85-98-3", "N,N-Diethylcarbanilide", 1.735, 1.265, 0, 1.346, 7.948, 1),
//	N_N_DIETHYLDODECAMIDE("14433-76-2", "N,N-Diethyldodecamide", 0.394, 0.934, 0, 0.966, 8.719, 0),
//	N_N_DIMETHYL_4_NITROANILINE("100-23-2", "N,N-Dimethyl-4-nitroaniline", 1.029, 1.917, 0, 0.339, 7.208, 1),
//	N_N_DIMETHYLANILINE("121-69-7", "N,N-Dimethylaniline", 0.865, 0.798, 0, 0.42, 4.842, 1),
//	N_N_DIPHENYL_4_PHENYLENEDIAMINE("74-31-7", "N,N-Diphenyl-4-phenylenediamine", 2.67, 1.847, 0.524, 1.106, 10.369, 32),
//	NONANAL("124-19-6", "Nonanal", 0.121, 0.635, 0, 0.399, 4.84, 0),
//	N_NONANE("111-84-2", "n-Nonane", 0, 0, 0, 0, 4.201, 0),
//	NONAN_1_OL("143-08-8", "Nonan-1-ol", 0.199, 0.406, 0.356, 0.542, 5.15, 0),
//	NOCTADECANE("593-45-3", "n-Octadecane", 0, 0, 0, 0, 8.722, 0),
//	OCTAMETHYLCYCLOTETRASILOXANE("556-67-2", "Octamethylcyclotetrasiloxane", -0.471, -0.084, 0, 0.513, 4.473, 0),
//	OCTAMETHYLTRISILOXANE("107-51-7", "Octamethyltrisiloxane", -0.498, -0.074, 0, 0.372, 3.936, 0),
//	OCTAN_1_OL("111-87-5", "Octan-1-ol", 0.199, 0.464, 0.327, 0.543, 4.635, 0),
//	OCTAN_2_OL("123-96-6", "Octan-2-ol", 0.176, 0.413, 0.275, 0.528, 4.335, 0),
//	OCTANAL("124-13-0", "Octanal", 0.148, 0.629, 0, 0.415, 4.364, 0),
//	N_OCTANE("111-65-9", "n-Octane", 0, 0, 0, 0, 3.689, 0),
//	OCTANOPHENONE("1674-37-9", "Octanophenone", 0.779, 0.995, 0, 0.498, 7.384, 1),
//	OCTYLTRIETHOXYSILANE("2943-75-1", "Octyltriethoxysilane", -0.255, -0.002, 0, 0.953, 6.986, 0),
//	PENTACHLOROPHENOL("87-86-5", "Pentachlorophenol", 1.732, 0.989, 0.554, 0.355, 7.525, 1),
//	PENTAN_1_OL("71-41-0", "Pentan-1-ol", 0.219, 0.44, 0.35, 0.526, 3.116, 0),
//	PENTAN_2_OL("6032-29-7", "Pentan-2-ol", 0.198, 0.387, 0.302, 0.564, 2.818, 0),
//	PENTAN_3_OL("584-02-1", "Pentan-3-ol", 0.218, 0.4, 0.275, 0.577, 2.833, 0),
//	PERYLENE("198-55-0", "Perylene", 2.896, 1.853, 0, 0.431, 11.652, 5),
//	PHENANTHRENE("85-01-8", "Phenanthrene", 1.961, 1.309, 0, 0.278, 7.709, 3),
//	PHENOL("108-95-2", "Phenol", 0.776, 0.772, 0.713, 0.317, 3.83, 1),
//	PHENYL_ACETATE("122-79-2", "Phenyl acetate", 0.661, 1.13, 0, 0.54, 4.414, 1),
//	PHENYLCYCLOHEXANE("827-52-1", "Phenylcyclohexane", 0.879, 0.59, 0, 0.239, 6.072, 1),
//	PHENYLSILANE("694-53-1", "Phenylsilane", 0.69, 0.407, 0, 0.148, 3.48, 1),
//	PHENYLTRIMETHOXYSILANE("2996-92-1", "Phenyltrimethoxysilane", 0.487, 0.804, 0, 0.687, 5.221, 1),
//	PHTHALIMIDE("85-41-6", "Phthalimide", 1.394, 1.561, 0.334, 0.591, 5.874, 0),
//	PHTHALONITRILE("91-15-6", "Phthalonitrile", 0.772, 1.953, 0, 0.391, 5.234, 0),
//	PROGESTERONE("57-83-0", "Progesterone", 1.585, 2.213, 0, 1.388, 11.665, 0),
//	PROPYL_ACETATE("109-60-4", "Propyl acetate", 0.092, 0.568, 0, 0.45, 2.887, 0),
//	PYRENE("129-00-0", "Pyrene", 2.27, 1.505, 0, 0.276, 8.966, 4),
//	PYRIDINE("110-86-1", "Pyridine", 0.63, 0.838, 0, 0.525, 3.011, 0),
//	QUININE("130-95-0", "Quinine", 1.832, 1.272, 0.808, 1.27, 12.025, 2),
//	QUINOLINE("91-22-5", "Quinoline", 1.415, 1.103, 0, 0.687, 5.302, 2),
//	STYRENE("100-42-5", "Styrene", 0.845, 0.671, 0, 0.166, 3.856, 1),
//	P_TERPHENYL("92-94-4", "p-Terphenyl", 1.816, 1.285, 0, 0.742, 9.513, 3),
//	TERPINEN_4_OL("562-74-3", "Terpinen-4-ol", 0.494, 0.523, 0.181, 0.626, 5.262, 0),
//	N_TETRADECANE("629-59-4", "n-Tetradecane", 0, 0, 0, 0, 6.655, 0),
//	TETRAHYDROFURAN("109-99-9", "Tetrahydrofuran", 0.295, 0.529, 0, 0.476, 2.541, 0),
//	THIOACETAMIDE("62-55-5", "Thioacetamide", 0.64, 1.816, 0.003, 0.293, 3.853, 0),
//	TOLUENE("108-88-3", "Toluene", 0.606, 0.508, 0, 0.139, 3.332, 1),
//	TRANS_STILBENE("103-30-0", "trans-Stilbene", 1.658, 1.219, 0, 0.296, 7.289, 2),
//	TRI_N_BUTYRIN("60-01-5", "Tri-n-Butyrin", 0.22, 1.235, 0, 1.292, 7.962, 0),
//	TRIBENZYLAMINE("620-40-6", "Tribenzylamine", 1.531, 1.196, 0, 0.599, 9.897, 3),
//	N_TRIDECANE("629-50-5", "n-Tridecane", 0, 0, 0, 0, 6.157, 0);
//
//
//	private final String name;
//	private final double E;
//	private final double S;
//	private final double A;
//	private final double B;
//	private final double L;
//	private final String casNumber;
//	private final int numBenzeneRings;
//	//    private final double relResponseFactor; // TODO: 4/5/2023 Relative compared to methyl Octanoate (IF NOT
//	// USE THE COMBUSTION ENTHALPY FOR EACH MOLECULE AS THE RRF PREDICTOR
//
//	Chemical(String casNumber, String name, double E, double S, double A, double B, double L, int numBenzeneRings) {
//		this.name = name;
//		this.E = E;
//		this.S = S;
//		this.A = A;
//		this.B = B;
//		this.L = L;
//		this.casNumber = casNumber;
//		this.numBenzeneRings = numBenzeneRings;
//	}
//
//	public String getName() {
//		return name;
//	}
//	public String getCAS(){return casNumber;}
//	@Override
//	public String toString(){
//		StringBuilder sb = new StringBuilder();
//		String result = sb.append(getName()).append(" = ").append(getCAS()).toString();
//		return result;
//	}
//}
//
//	//CALCULATE VALUES FOR PEAK GENERATION
////    public double calcAlpha(){
////        return 0.0;
////    }
////    public double calcMu(){
////        return 0.0 + ChromatographySimulator.currentTime();
////    }
////    public double calcGamma(){
////        return 0.0;
////    }
//
////    abstract double calcRetentionFactor(); // TODO: 4/5/2023 implement using the LSER constants
////    Retention Factor Model: The retention factor (k) can be used to model elution order, incorporating the interaction between the compounds and the stationary phase. The retention factor is defined as:
////    k = (tR - tM) / tM
////
////    where tR is the retention time of the compound, and tM is the retention time of an unretained compound (also called the dead time). The retention factor is related to the partition coefficient (K) of the compound between the stationary and mobile phases:
////
////    k = (V_s / V_m) × K
////
////    where V_s is the volume of the stationary phase and V_m is the volume of the mobile phase. Compounds with lower retention factors will elute faster.
//
//
////