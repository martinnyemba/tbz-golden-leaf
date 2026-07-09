import React from "react";
import { StyleSheet, View, Pressable, Platform, ScrollView } from "react-native";
import { useRouter } from "expo-router";
import MaterialIcons from "@expo/vector-icons/MaterialIcons";
import { ThemedText } from "@/components/themed-text";
import { ThemedView } from "@/components/themed-view";
import { Colors } from "@/constants/theme";
import { FontFamily, FontWeight } from "@/constants/typography";
import { useColorScheme } from "@/hooks/use-color-scheme";

export default function GuidelinesScreen() {
  const router = useRouter();
  const theme = (useColorScheme() ?? "light") === "dark" ? "dark" : "light";

  return (
    <ThemedView style={styles.container}>
      <View style={[styles.header, { backgroundColor: Colors[theme].primary }]}>
        <Pressable onPress={() => router.back()} style={styles.backButton}>
          <MaterialIcons name="chevron-left" size={32} color={Colors[theme].surface} />
        </Pressable>
        <ThemedText style={[styles.headerTitle, { color: Colors[theme].surface }]} numberOfLines={1} ellipsizeMode="tail">
          TBZ Guidelines
        </ThemedText>
        <View style={{ width: 48 }} />
      </View>
      <ScrollView contentContainerStyle={styles.content}>
        <View style={styles.iconBox}>
          <MaterialIcons name="assignment-turned-in" size={64} color={Colors[theme].primary} />
        </View>
        <ThemedText style={styles.title}>Regulatory Framework</ThemedText>
        
        <View style={[styles.card, { backgroundColor: Colors[theme].surface, borderColor: Colors[theme].border }]}>
          <View style={styles.cardHeader}>
            <MaterialIcons name="format-list-numbered" size={24} color={Colors[theme].primary} />
            <ThemedText style={styles.cardTitle} numberOfLines={2} ellipsizeMode="tail">
              1. Registration Mandates
            </ThemedText>
          </View>
          <ThemedText style={styles.cardBody}>
            All tobacco growers must ensure applications (Form 2) are filed and processed before the onset of the farming season. Verified hectares under cultivation must align strictly with initial quotas to mitigate overproduction fines.
          </ThemedText>
        </View>

        <View style={[styles.card, { backgroundColor: Colors[theme].surface, borderColor: Colors[theme].border }]}>
          <View style={styles.cardHeader}>
            <MaterialIcons name="local-shipping" size={24} color={Colors[theme].primary} />
            <ThemedText style={styles.cardTitle} numberOfLines={2} ellipsizeMode="tail">
              2. Transport Permits
            </ThemedText>
          </View>
          <ThemedText style={styles.cardBody}>
            Movement of all prepared bales legally requires a generated Transport Permit validated by the TBZ authorities. Vehicles transporting unregistered tobacco are subject to impoundment.
          </ThemedText>
        </View>

        <View style={[styles.card, { backgroundColor: Colors[theme].surface, borderColor: Colors[theme].border }]}>
          <View style={styles.cardHeader}>
            <MaterialIcons name="eco" size={24} color={Colors[theme].primary} />
            <ThemedText style={styles.cardTitle} numberOfLines={2} ellipsizeMode="tail">
              3. Field Inspections
            </ThemedText>
          </View>
          <ThemedText style={styles.cardBody}>
            Inspectors retain universal clearance to perform random site validations to cross-reference crop estimates prior to harvest. Ensure all GPS coordinates provided match specific plot lines organically.
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
  content: { padding: 24, paddingBottom: 80 },
  iconBox: { alignItems: "center", marginBottom: 16, marginTop: 10 },
  title: { fontSize: 24, fontFamily: FontFamily.serif, fontWeight: FontWeight.bold, textAlign: "center", marginBottom: 32 },
  card: { borderWidth: 1, borderRadius: 12, padding: 16, marginBottom: 16, overflow: "hidden" },
  cardHeader: { flexDirection: "row", alignItems: "center", marginBottom: 8, gap: 12, flexWrap: "wrap" },
  cardTitle: { fontSize: 18, fontFamily: FontFamily.serif, fontWeight: FontWeight.medium, flex: 1 },
  cardBody: { fontSize: 15, fontFamily: FontFamily.serif, fontWeight: FontWeight.regular, lineHeight: 22, color: '#6B7280' }
});
