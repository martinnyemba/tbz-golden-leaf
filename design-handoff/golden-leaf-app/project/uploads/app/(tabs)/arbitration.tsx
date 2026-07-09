import { CameraView, useCameraPermissions, type BarcodeScanningResult } from "expo-camera";
import { useState } from "react";
import { Alert, Modal, Pressable, ScrollView, StyleSheet, View } from "react-native";

import { ThemedText } from "@/components/themed-text";
import { ThemedView } from "@/components/themed-view";
import { PrimaryButton, SelectField, TextField } from "@/components/ui/form-controls";

const REJECTION_REASONS = [
  "Nested",
  "High Moisture",
  "Low Moisture",
  "NTRM",
  "Overweight",
  "Underweight",
  "No sale",
];

export default function ArbitrationScreen() {
  const [permission, requestPermission] = useCameraPermissions();
  const [scanOpen, setScanOpen] = useState(false);

  const [arbitratorName, setArbitratorName] = useState("");
  const [growerId, setGrowerId] = useState("");
  const [baleId, setBaleId] = useState("");
  const [grade, setGrade] = useState("");
  const [date, setDate] = useState("");
  const [rejected, setRejected] = useState<"" | "Yes" | "No">("");
  const [rejectionReason, setRejectionReason] = useState("");

  const canScan = permission?.granted === true;

  return (
    <ThemedView style={styles.container}>
      <ScrollView contentContainerStyle={styles.content}>

        <View style={styles.section}>
          <ThemedText type="subtitle">Audit Trail</ThemedText>
          <TextField
            label="Arbitrator Name"
            value={arbitratorName}
            onChangeText={setArbitratorName}
            required
          />
        </View>

        <View style={styles.section}>
          <ThemedText type="subtitle">Bale</ThemedText>
          <PrimaryButton
            title="Scan Bale ID barcode"
            onPress={async () => {
              if (!permission) return;
              if (!permission.granted) {
                const res = await requestPermission();
                if (!res.granted) {
                  Alert.alert("Camera permission denied", "Enable camera permission to scan barcodes.");
                  return;
                }
              }
              setScanOpen(true);
            }}
          />
          <TextField label="Grower ID" value={growerId} onChangeText={setGrowerId} required />
          <TextField label="Bale ID" value={baleId} onChangeText={setBaleId} required />
          <TextField label="Grade" value={grade} onChangeText={setGrade} />
          <TextField label="Date" value={date} onChangeText={setDate} placeholder="YYYY-MM-DD" required />
          <SelectField
            label="Rejected"
            value={rejected}
            onChange={(v) => {
              setRejected(v as "" | "Yes" | "No");
              if (v !== "Yes") setRejectionReason("");
            }}
            options={["", "Yes", "No"].filter(Boolean)}
            required
          />
          {rejected === "Yes" ? (
            <SelectField
              label="Reason of rejection"
              value={rejectionReason}
              onChange={setRejectionReason}
              options={REJECTION_REASONS}
              required
            />
          ) : null}
        </View>

        <PrimaryButton
          title="Submit Arbitration"
          onPress={() => {
            if (!arbitratorName.trim()) {
              Alert.alert("Missing arbitrator name", "Arbitrator name is required for audit trail.");
              return;
            }
            const missing: string[] = [];
            if (!growerId.trim()) missing.push("Grower ID");
            if (!baleId.trim()) missing.push("Bale ID");
            if (!date.trim()) missing.push("Date");
            if (!rejected) missing.push("Rejected");

            if (missing.length > 0) {
              Alert.alert("Missing fields", `Fill all required fields before submitting:\n\n${missing.join("\n")}`);
              return;
            }
            if (rejected === "Yes" && !rejectionReason) {
              Alert.alert("Missing rejection reason", "Select a rejection reason.");
              return;
            }
            Alert.alert("Submitted", `Submitted by ${arbitratorName} on ${new Date().toLocaleString()}.`);
          }}
        />
      </ScrollView>

      <Modal visible={scanOpen} animationType="slide" onRequestClose={() => setScanOpen(false)}>
        <ThemedView style={styles.scanContainer}>
          <View style={styles.scanHeader}>
            <ThemedText type="subtitle">Scan Bale ID</ThemedText>
            <Pressable onPress={() => setScanOpen(false)} style={styles.scanClose}>
              <ThemedText type="link">Close</ThemedText>
            </Pressable>
          </View>
          {canScan ? (
            <CameraView
              style={styles.camera}
              onBarcodeScanned={(result: BarcodeScanningResult) => {
                setBaleId(result.data);
                setScanOpen(false);
              }}
            />
          ) : (
            <View style={styles.scanBody}>
              <ThemedText>Camera permission is required to scan.</ThemedText>
              <PrimaryButton title="Request permission" onPress={async () => { await requestPermission(); }} />
            </View>
          )}
        </ThemedView>
      </Modal>
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
  scanContainer: {
    flex: 1,
  },
  scanHeader: {
    paddingHorizontal: 16,
    paddingVertical: 14,
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "center",
  },
  scanClose: {
    paddingHorizontal: 8,
    paddingVertical: 6,
  },
  scanBody: {
    flex: 1,
    padding: 20,
    gap: 14,
    justifyContent: "center",
  },
  camera: {
    flex: 1,
  },
});
