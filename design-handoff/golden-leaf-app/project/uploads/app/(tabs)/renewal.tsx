import { useMemo, useState } from "react";
import { Alert, ScrollView, StyleSheet, View } from "react-native";

import { ThemedText } from "@/components/themed-text";
import { ThemedView } from "@/components/themed-view";
import {
    PrimaryButton,
    SelectField,
    TextField,
} from "@/components/ui/form-controls";

const SPONSORS = ["Self-sponsorship", "Sponsor A", "Sponsor B"];
const CROP_TYPES = ["Flue Cured Tobacco", "Burley", "Dark Fired Tobacco"];
const BARN_TYPES = ["Traditional", "Modern", "Other"];

function yieldPerHaForHectarage(hectarage: number) {
  if (!Number.isFinite(hectarage) || hectarage <= 0) return "";
  if (hectarage >= 10) return "3000";
  return "1500";
}

export default function RenewalScreen() {
  const [inspectorName, setInspectorName] = useState("");
  const [nrc, setNrc] = useState("");
  const [sponsor, setSponsor] = useState("");
  const [cropType, setCropType] = useState("");
  const [hectarage, setHectarage] = useState("");
  const [barnCount, setBarnCount] = useState("");
  const [barnType, setBarnType] = useState("");
  const [stringsPerBarn, setStringsPerBarn] = useState("");
  const [gps, setGps] = useState("");

  const computedYield = useMemo(() => {
    const n = Number(hectarage);
    return yieldPerHaForHectarage(n);
  }, [hectarage]);

  return (
    <ThemedView style={styles.container}>
      <ScrollView contentContainerStyle={styles.content}>
        <ThemedText>
          Update or renew a grower record and crop information.
        </ThemedText>

        <View style={styles.section}>
          <ThemedText type="subtitle">Audit Trail</ThemedText>
          <TextField
            label="Inspector Name"
            value={inspectorName}
            onChangeText={setInspectorName}
            placeholder="e.g., Jane Banda"
            required
          />
        </View>

        <View style={styles.section}>
          <ThemedText type="subtitle">Crop Information</ThemedText>
          <TextField
            label="NRC Number"
            value={nrc}
            onChangeText={setNrc}
            placeholder="e.g., 123456/10/1"
            required
          />
          <SelectField
            label="Sponsor"
            value={sponsor}
            onChange={setSponsor}
            options={SPONSORS}
            required
          />
          <SelectField
            label="Crop Type"
            value={cropType}
            onChange={setCropType}
            options={CROP_TYPES}
            required
          />
          <TextField
            label="Hectarage"
            value={hectarage}
            onChangeText={setHectarage}
            placeholder="e.g., 0.5"
            keyboardType="decimal-pad"
            required
          />
          <TextField
            label="Yield per Ha (Kg)"
            value={computedYield}
            onChangeText={() => {}}
            editable={false}
            placeholder="Auto-calculated"
            required
          />
          <TextField
            label="Number of Barns"
            value={barnCount}
            onChangeText={setBarnCount}
            keyboardType="number-pad"
          />
          <SelectField
            label="Types of Barns"
            value={barnType}
            onChange={setBarnType}
            options={BARN_TYPES}
          />
          <TextField
            label="Number of Strings in a Barn(s)"
            value={stringsPerBarn}
            onChangeText={setStringsPerBarn}
            keyboardType="number-pad"
          />
          <TextField
            label="GPS Coordinates"
            value={gps}
            onChangeText={setGps}
            placeholder="Auto-captured"
          />
        </View>

        <PrimaryButton
          title="Submit registration update / renewal"
          onPress={() => {
            if (!inspectorName.trim()) {
              Alert.alert(
                "Missing inspector name",
                "Inspector name is required for audit trail.",
              );
              return;
            }
            const missing: string[] = [];
            if (!nrc.trim()) missing.push("NRC Number");
            if (!sponsor) missing.push("Sponsor");
            if (!cropType) missing.push("Crop Type");
            if (!hectarage.trim()) missing.push("Hectarage");

            if (missing.length > 0) {
              Alert.alert(
                "Missing fields",
                `Please complete all required fields before submitting:\n\n${missing.join("\n")}`,
              );
              return;
            }
            Alert.alert(
              "Submitted",
              `Submitted by ${inspectorName} on ${new Date().toLocaleString()}.`,
            );
          }}
        />
      </ScrollView>
    </ThemedView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  content: {
    padding: 20,
    gap: 14,
  },
  section: {
    gap: 10,
  },
});
