import React from "react";
import { StyleSheet, View, Pressable, Platform, ScrollView, Image } from "react-native";
import { useRouter } from "expo-router";
import MaterialIcons from "@expo/vector-icons/MaterialIcons";
import { ThemedText } from "@/components/themed-text";
import { ThemedView } from "@/components/themed-view";
import { Colors } from "@/constants/theme";
import { FontFamily, FontWeight } from "@/constants/typography";
import { useColorScheme } from "@/hooks/use-color-scheme";

export default function AboutAppScreen() {
  const router = useRouter();
  const theme = (useColorScheme() ?? "light") === "dark" ? "dark" : "light";

  return (
    <ThemedView style={styles.container}>
      <View style={[styles.header, { backgroundColor: Colors[theme].primary }]}>
        <Pressable onPress={() => router.back()} style={styles.backButton}>
          <MaterialIcons name="chevron-left" size={32} color={Colors[theme].surface} />
        </Pressable>
        <ThemedText style={[styles.headerTitle, { color: Colors[theme].surface }]}>About App</ThemedText>
        <View style={{ width: 48 }} />
      </View>
      <ScrollView contentContainerStyle={styles.content}>
        <View style={styles.logoBox}>
          <Image source={require("../assets/images/icon.png")} style={styles.appLogo} resizeMode="contain" />
          <ThemedText style={styles.appName}>Golden leaf</ThemedText>
          <ThemedText style={styles.version}>Version 1.0.0</ThemedText>
        </View>
        <View style={styles.section}>
          <ThemedText style={styles.heading}>About the App</ThemedText>
          <ThemedText style={styles.paragraph}>
            Golden leaf mobile application is a Tobacco Board of Zambia (TBZ) application used to support tobacco registration, marketing, and regulatory
            compliance within the tobacco value chain.
          </ThemedText>
        </View>

        <View style={styles.section}>
          <ThemedText style={styles.heading}>TRMCS</ThemedText>
          <ThemedText style={styles.paragraph}>
            The Tobacco Registration, Marketing & Compliance System (TRMCS) is the platform that supports licensing and compliance verification, market
            monitoring, analytics, and enforcement workflows. Use is mandatory for regulated participants.
          </ThemedText>
        </View>

        <View style={styles.section}>
          <ThemedText style={styles.heading}>Compliance and Enforcement</ThemedText>
          <ThemedText style={styles.bullets}>
            • TBZ may audit system activities and access submitted data{"\n"}• TBZ may conduct investigations without notice{"\n"}• System records may be
            relied upon in legal and regulatory proceedings
          </ThemedText>
        </View>

        <View style={styles.section}>
          <ThemedText style={styles.heading}>Key Features</ThemedText>
          <ThemedText style={styles.bullets}>
            • Registration and compliance workflows{"\n"}• Permit validation and related enforcement workflows{"\n"}• Market monitoring and analytics{"\n"}•
            Offline capability (where available) with later synchronization when internet is restored
          </ThemedText>
        </View>
      </ScrollView>
    </ThemedView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  header: { flexDirection: "row", alignItems: "center", justifyContent: "space-between", paddingTop: Platform.OS === "android" ? 40 : 60, paddingBottom: 16, paddingHorizontal: 8 },
  backButton: { padding: 8, width: 48, alignItems: "center" },
  headerTitle: { fontSize: 22, fontFamily: FontFamily.serif, fontWeight: FontWeight.regular },
  content: { padding: 24, alignItems: "center" },
  logoBox: { alignItems: "center", marginTop: 40, marginBottom: 30 },
  appLogo: { width: 120, height: 120 },
  appName: { fontSize: 24, fontFamily: FontFamily.serif, fontWeight: FontWeight.bold, marginTop: 16 },
  version: { fontSize: 14, fontFamily: FontFamily.serif, fontWeight: FontWeight.regular, color: '#6B7280', marginTop: 4 },
  section: { width: "100%", marginTop: 14 },
  heading: { fontSize: 18, fontFamily: FontFamily.serif, fontWeight: FontWeight.medium, marginBottom: 8 },
  paragraph: { fontSize: 15, fontFamily: FontFamily.serif, fontWeight: FontWeight.regular, lineHeight: 22, color: "#4B5563" },
  bullets: { fontSize: 15, fontFamily: FontFamily.serif, fontWeight: FontWeight.regular, lineHeight: 22, color: "#4B5563" },
});
