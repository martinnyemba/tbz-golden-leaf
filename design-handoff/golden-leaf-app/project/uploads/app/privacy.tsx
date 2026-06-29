import React from "react";
import { StyleSheet, View, Pressable, Platform, ScrollView, Image } from "react-native";
import { useRouter } from "expo-router";
import MaterialIcons from "@expo/vector-icons/MaterialIcons";
import { ThemedText } from "@/components/themed-text";
import { ThemedView } from "@/components/themed-view";
import { Colors } from "@/constants/theme";
import { FontFamily, FontWeight } from "@/constants/typography";
import { useColorScheme } from "@/hooks/use-color-scheme";

export default function PrivacyScreen() {
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
        <ThemedText style={styles.title}>Golden leaf mobile application — Privacy policy</ThemedText>

        <ThemedText style={styles.heading}>1. Legal Basis for Processing</ThemedText>
        <ThemedText style={styles.bullets}>
          • Statutory obligation{"\n"}• Public interest mandate{"\n"}• Consent (where applicable)
        </ThemedText>

        <ThemedText style={styles.heading}>2. Mandatory Data Collection</ThemedText>
        <ThemedText style={styles.paragraph}>
          Certain data is mandatory by law. Failure to provide such data may result in denial of access and regulatory non‑compliance status.
        </ThemedText>

        <ThemedText style={styles.heading}>3. Lawful Disclosure</ThemedText>
        <ThemedText style={styles.bullets}>
          • Required by law{"\n"}• Requested by competent authorities{"\n"}• Necessary for enforcement of regulatory functions
        </ThemedText>

        <ThemedText style={styles.heading}>4. Cross‑Border Processing</ThemedText>
        <ThemedText style={styles.bullets}>
          • Adequate safeguards SHALL be implemented{"\n"}• Transfers SHALL comply with statutory restrictions
        </ThemedText>

        <ThemedText style={styles.heading}>5. Data Subject Rights (Conditional)</ThemedText>
        <ThemedText style={styles.paragraph}>
          Rights of access, correction, or deletion are subject to statutory limitations and may be restricted where data is required for regulatory
          enforcement.
        </ThemedText>

        <ThemedText style={styles.heading}>6. Retention and Legal Hold</ThemedText>
        <ThemedText style={styles.bullets}>
          • For statutory periods{"\n"}• Indefinitely where required for investigations, audits, or litigation
        </ThemedText>

        <ThemedText style={styles.heading}>7. Security Disclaimer</ThemedText>
        <ThemedText style={styles.paragraph}>
          While TBZ implements robust safeguards, no system is completely secure. Users acknowledge inherent cybersecurity risks.
        </ThemedText>

        <ThemedText style={styles.title}>TRMCS — Privacy policy (legal version)</ThemedText>

        <ThemedText style={styles.heading}>1. Mandatory Regulatory Data Processing</ThemedText>
        <ThemedText style={styles.paragraph}>
          All personal data processed under TRMCS is required by law and is not optional for regulated participants.
        </ThemedText>

        <ThemedText style={styles.heading}>2. Disclosure Without Consent</ThemedText>
        <ThemedText style={styles.bullets}>
          • For enforcement purposes{"\n"}• Under court orders{"\n"}• To authorized regulatory bodies
        </ThemedText>

        <ThemedText style={styles.heading}>3. Retention and Archival</ThemedText>
        <ThemedText style={styles.bullets}>
          • Statutory minimum periods{"\n"}• Extended where required for audits or investigations
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
