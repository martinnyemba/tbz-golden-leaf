import React from "react";
import { StyleSheet, View, Pressable, Platform, ScrollView, Image } from "react-native";
import { useRouter } from "expo-router";
import MaterialIcons from "@expo/vector-icons/MaterialIcons";
import { ThemedText } from "@/components/themed-text";
import { ThemedView } from "@/components/themed-view";
import { Colors } from "@/constants/theme";
import { FontFamily, FontWeight } from "@/constants/typography";
import { useColorScheme } from "@/hooks/use-color-scheme";

export default function TermsScreen() {
  const router = useRouter();
  const theme = (useColorScheme() ?? "light") === "dark" ? "dark" : "light";

  return (
    <ThemedView style={styles.container}>
      <View style={[styles.header, { backgroundColor: Colors[theme].primary }]}>
        <Pressable onPress={() => router.back()} style={styles.backButton}>
          <MaterialIcons name="chevron-left" size={32} color={Colors[theme].surface} />
        </Pressable>
        <Image
          source={require("../assets/images/android-icon-monochrome.png")}
          style={[styles.headerLogo, { tintColor: Colors[theme].surface }]}
          resizeMode="contain"
        />
        <View style={{ width: 48 }} />
      </View>
      <ScrollView contentContainerStyle={styles.content}>
        <ThemedText style={styles.title}>Golden leaf mobile application — Terms of use</ThemedText>

        <ThemedText style={styles.heading}>1. Binding Agreement</ThemedText>
        <ThemedText style={styles.paragraph}>
          These Terms constitute a legally binding agreement between the user (“User”) and the Tobacco Board of Zambia (“TBZ”). By accessing or using
          the Application, the User irrevocably agrees to these Terms. If the User does not agree, they must immediately discontinue use.
        </ThemedText>

        <ThemedText style={styles.heading}>2. Legal Capacity</ThemedText>
        <ThemedText style={styles.bullets}>
          • Are legally competent under Zambian law{"\n"}• Have authority to act (where acting on behalf of an entity){"\n"}• Are duly registered within
          the tobacco value chain where required
        </ThemedText>

        <ThemedText style={styles.heading}>3. Regulatory Compliance Obligation</ThemedText>
        <ThemedText style={styles.paragraph}>
          Use of the Application constitutes mandatory regulatory compliance. Users shall comply with all TBZ directives, applicable statutes and
          regulations, and licensing and reporting obligations. Failure constitutes a regulatory breach subject to enforcement action.
        </ThemedText>

        <ThemedText style={styles.heading}>4. User Representations and Warranties</ThemedText>
        <ThemedText style={styles.bullets}>
          • All data submitted is true, accurate, and complete{"\n"}• No fraudulent, misleading, or manipulated data{"\n"}• No system abuse or unauthorized
          access
        </ThemedText>
        <ThemedText style={styles.paragraph}>Breach of this clause constitutes a material breach.</ThemedText>

        <ThemedText style={styles.heading}>5. System Integrity and Prohibited Conduct</ThemedText>
        <ThemedText style={styles.bullets}>
          • No unauthorized access, reverse engineering, or penetration{"\n"}• No malware or malicious code{"\n"}• No circumvention of controls or
          compliance mechanisms
        </ThemedText>
        <ThemedText style={styles.paragraph}>
          Such acts may trigger immediate termination and criminal prosecution under applicable law.
        </ThemedText>

        <ThemedText style={styles.heading}>6. Suspension and Termination</ThemedText>
        <ThemedText style={styles.paragraph}>
          TBZ reserves the unilateral right to suspend or terminate access without prior notice and to report violations to regulatory or law enforcement
          authorities.
        </ThemedText>

        <ThemedText style={styles.heading}>7. Limitation of Liability</ThemedText>
        <ThemedText style={styles.bullets}>
          • TBZ disclaims all indirect, incidental, or consequential damages (to the fullest extent permitted by law){"\n"}• Liability is limited to direct
          damages only, where legally applicable
        </ThemedText>

        <ThemedText style={styles.heading}>8. Indemnity</ThemedText>
        <ThemedText style={styles.bullets}>
          • Claims arising from misuse{"\n"}• Breach of these Terms{"\n"}• Violation of laws or third‑party rights
        </ThemedText>

        <ThemedText style={styles.heading}>9. Governing Law and Jurisdiction</ThemedText>
        <ThemedText style={styles.paragraph}>
          These Terms shall be governed by the laws of the Republic of Zambia. Disputes shall be subject to the exclusive jurisdiction of the courts of
          Zambia.
        </ThemedText>

        <ThemedText style={styles.title}>TRMCS — TERMS OF USE</ThemedText>

        <ThemedText style={styles.heading}>1. Legal Status of System</ThemedText>
        <ThemedText style={styles.paragraph}>TRMCS is a platform. Use is mandatory for all regulated participants.</ThemedText>

        <ThemedText style={styles.heading}>2. Compliance Obligation</ThemedText>
        <ThemedText style={styles.bullets}>
          • Submit accurate transactional and compliance data{"\n"}• Adhere strictly to system workflows{"\n"}• Failure constitutes regulatory
          non‑compliance
        </ThemedText>

        <ThemedText style={styles.heading}>3. Audit Rights</ThemedText>
        <ThemedText style={styles.bullets}>
          • Audit all user activities{"\n"}• Access all submitted data{"\n"}• Conduct investigations without notice
        </ThemedText>

        <ThemedText style={styles.heading}>4. Enforcement Powers</ThemedText>
        <ThemedText style={styles.bullets}>
          • Suspend licenses{"\n"}• Block system access{"\n"}• Initiate legal proceedings
        </ThemedText>

        <ThemedText style={styles.heading}>5. Evidence Clause</ThemedText>
        <ThemedText style={styles.paragraph}>
          System records SHALL constitute admissible evidence and be relied upon in legal and regulatory proceedings.
        </ThemedText>

        <ThemedText style={styles.heading}>6. Liability Limitation</ThemedText>
        <ThemedText style={styles.bullets}>
          • TBZ SHALL NOT be liable for business losses due to regulatory enforcement{"\n"}• TBZ SHALL NOT be liable for system downtime where reasonable
          measures exist
        </ThemedText>
      </ScrollView>
    </ThemedView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  header: { flexDirection: "row", alignItems: "center", justifyContent: "space-between", paddingTop: Platform.OS === "android" ? 40 : 60, paddingBottom: 16, paddingHorizontal: 8 },
  backButton: { padding: 8, width: 48, alignItems: "center" },
  headerTitle: { fontSize: 22, fontFamily: FontFamily.serif, fontWeight: FontWeight.regular },
  headerLogo: { width: 34, height: 34 },
  content: { padding: 24 },
  title: { fontSize: 14, fontFamily: FontFamily.serif, fontWeight: FontWeight.bold, color: "#0B6B3A", marginTop: 6, marginBottom: 10, letterSpacing: 0.5 },
  heading: { fontSize: 18, fontFamily: FontFamily.serif, fontWeight: FontWeight.medium, marginTop: 16, marginBottom: 8 },
  paragraph: { fontSize: 15, fontFamily: FontFamily.serif, fontWeight: FontWeight.regular, lineHeight: 22, color: "#4B5563" },
  bullets: { fontSize: 15, fontFamily: FontFamily.serif, fontWeight: FontWeight.regular, lineHeight: 22, color: "#4B5563" },
});
