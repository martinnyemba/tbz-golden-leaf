import { useLocalSearchParams, useRouter } from "expo-router";
import * as Location from "expo-location";
import { useEffect, useMemo, useState } from "react";
import { Alert, ScrollView, StyleSheet, View } from "react-native";

import { ThemedText } from "@/components/themed-text";
import { ThemedView } from "@/components/themed-view";
import {
    PrimaryButton,
    SelectField,
    TextField,
} from "@/components/ui/form-controls";
import {
    saveInspectionReport,
    getDeviceId,
    type CuringInspection,
    type GrowerProfile,
    type InspectionReport,
} from "@/lib/inspection-storage";

const BARN_TYPES = ["Flue Cured Barn", "Bulk Cure Barn", "Air Cured Barn", "Fire Cured Barn"];
const FUEL_SOURCES = ["Wood", "Coal", "Gas", "Electric", "Other"];
const CURING_STATUSES = ["Not Started", "In Progress", "Completed"];
const LEAF_QUALITY = ["Good", "Fair", "Poor"];
const GRADING_STATUS = ["Not Graded", "In Progress", "Graded"];

export default function CuringInspectionScreen() {
  const router = useRouter();
  const params = useLocalSearchParams<{
    inspectorName?: string;
    stage?: string;
    grower?: string;
    gps?: string;
    deviceId?: string;
    inspectionId?: string;
    scheduledDate?: string;
    inspectionType?: string;
    editingReportId?: string;
  }>();

  const grower = useMemo(() => {
    try {
      return params.grower
        ? (JSON.parse(params.grower) as GrowerProfile)
        : null;
    } catch {
      return null;
    }
  }, [params.grower]);

  const inspectionId = (params.inspectionId ?? "").toString();
  const scheduledDate = (params.scheduledDate ?? "").toString();
  const inspectionType = (params.inspectionType ?? "").toString();

  const [inspectorName] = useState((params.inspectorName ?? "").toString());
  const [gps, setGps] = useState((params.gps ?? "").toString());
  const [deviceId, setDeviceId] = useState((params.deviceId ?? "").toString());
  const [locationError, setLocationError] = useState("");

  useEffect(() => {
    if (deviceId) return;
    (async () => {
      const id = await getDeviceId();
      setDeviceId(id);
    })();
  }, [deviceId]);

  useEffect(() => {
    if (gps.trim()) return;
    (async () => {
      try {
        const { status } = await Location.requestForegroundPermissionsAsync();
        if (status !== "granted") {
          setLocationError("Location permission denied.");
          return;
        }
        setLocationError("");
        try {
          const pos = await Location.getCurrentPositionAsync({});
          setGps(`${pos.coords.latitude.toFixed(6)}, ${pos.coords.longitude.toFixed(6)}`);
        } catch {
          const last = await Location.getLastKnownPositionAsync();
          if (last?.coords) {
            setGps(`${last.coords.latitude.toFixed(6)}, ${last.coords.longitude.toFixed(6)}`);
          } else {
            setLocationError("Current location is unavailable. Make sure that location services are enabled.");
          }
        }
      } catch {
        setLocationError("Current location is unavailable. Make sure that location services are enabled.");
      }
    })();
  }, [gps]);

  const [numberOfBarns, setNumberOfBarns] = useState("");
  const [typeOfBarns, setTypeOfBarns] = useState("");
  const [curingCycles, setCuringCycles] = useState("");
  const [fuelSource, setFuelSource] = useState("");
  const [curingStatus, setCuringStatus] = useState("");
  const [leafQuality, setLeafQuality] = useState("");
  const [gradingStatus, setGradingStatus] = useState("");
  const [inspectorRemarks, setInspectorRemarks] = useState("");

  useEffect(() => {
    if (!params.editingReportId) return;
    import("@/lib/inspection-storage").then((module) => {
      module.listInspectionReports().then((reports) => {
        const report = reports.find((r) => r.id === params.editingReportId);
        if (!report || report.stage !== "Curing") return;
        const p = report.payload as CuringInspection;
        setNumberOfBarns(p.numberOfBarns);
        setTypeOfBarns(p.typeOfBarns);
        setCuringCycles(p.curingCycles);
        setFuelSource(p.fuelSource);
        setCuringStatus(p.curingStatus);
        setLeafQuality(p.leafQuality);
        setGradingStatus(p.gradingStatus);
        setInspectorRemarks(p.inspectorRemarks);
      });
    });
  }, [params.editingReportId]);

  const canSubmit = useMemo(() => {
    return Boolean(
      grower &&
      inspectorName.trim() &&
      gps.trim() &&
      deviceId &&
      numberOfBarns.trim() &&
      typeOfBarns &&
      curingCycles.trim() &&
      fuelSource.trim() &&
      curingStatus &&
      leafQuality &&
      gradingStatus &&
      inspectorRemarks.trim(),
    );
  }, [
    curingCycles,
    curingStatus,
    deviceId,
    fuelSource,
    gps,
    gradingStatus,
    grower,
    inspectorName,
    inspectorRemarks,
    leafQuality,
    numberOfBarns,
    typeOfBarns,
  ]);

  const missingFields = useMemo(() => {
    const missing: string[] = [];
    if (!grower) missing.push("Grower");
    if (!inspectorName.trim()) missing.push("Inspector Name");
    if (!gps.trim()) missing.push("GPS Coordinates");
    if (!deviceId) missing.push("Device ID");
    if (!numberOfBarns.trim()) missing.push("Number of Barns");
    if (!typeOfBarns) missing.push("Type of Barns");
    if (!curingCycles.trim()) missing.push("Curing Cycles");
    if (!fuelSource.trim()) missing.push("Fuel Source");
    if (!curingStatus) missing.push("Curing Status");
    if (!leafQuality) missing.push("Leaf Quality");
    if (!gradingStatus) missing.push("Grading Status");
    if (!inspectorRemarks.trim()) missing.push("Inspector Remarks");
    return missing;
  }, [
    curingCycles,
    curingStatus,
    deviceId,
    fuelSource,
    gps,
    gradingStatus,
    grower,
    inspectorName,
    inspectorRemarks,
    leafQuality,
    numberOfBarns,
    typeOfBarns,
  ]);

  return (
    <ThemedView style={styles.container}>
      <ScrollView contentContainerStyle={styles.content}>

        <View style={styles.section}>
          <ThemedText type="subtitle">Grower</ThemedText>
          <ThemedText>
            {grower
              ? `${grower.name} (${grower.growerId})`
              : "No grower loaded"}
          </ThemedText>
          {grower ? (
            <ThemedText>
              {grower.province} / {grower.district} • Sponsor: {grower.sponsor || "-"} • Hectarage: {grower.hectarage || 0}
            </ThemedText>
          ) : null}
        </View>

        <View style={styles.section}>
          <ThemedText type="subtitle">{inspectionId ? "Scheduled Inspection" : "Inspection Context"}</ThemedText>
          {inspectionId ? <ThemedText>Inspection: {inspectionId}</ThemedText> : null}
          {inspectionType ? <ThemedText>Type: {inspectionType}</ThemedText> : null}
          {scheduledDate ? <ThemedText>Scheduled Date: {scheduledDate}</ThemedText> : null}
          <ThemedText>Inspector: {inspectorName || "—"}</ThemedText>
          {!gps.trim() || locationError ? (
            <ThemedText>{locationError || "Capturing GPS..."}</ThemedText>
          ) : (
            <ThemedText>GPS: {gps}</ThemedText>
          )}
        </View>

        <View style={styles.section}>
          <ThemedText type="subtitle">Inspection Form</ThemedText>
          <TextField
            label="Number of Barns"
            value={numberOfBarns}
            onChangeText={setNumberOfBarns}
            keyboardType="number-pad"
            required
          />
          <SelectField
            label="Type of Barns"
            value={typeOfBarns}
            onChange={setTypeOfBarns}
            options={BARN_TYPES}
            required
          />
          <TextField
            label="Curing Cycles"
            value={curingCycles}
            onChangeText={setCuringCycles}
            keyboardType="number-pad"
            required
          />
          <SelectField
            label="Fuel Source"
            value={fuelSource}
            onChange={setFuelSource}
            options={FUEL_SOURCES}
            required
          />
          <SelectField
            label="Curing Status"
            value={curingStatus}
            onChange={setCuringStatus}
            options={CURING_STATUSES}
            required
          />
          <SelectField
            label="Leaf Quality"
            value={leafQuality}
            onChange={setLeafQuality}
            options={LEAF_QUALITY}
            required
          />
          <SelectField
            label="Grading Status"
            value={gradingStatus}
            onChange={setGradingStatus}
            options={GRADING_STATUS}
            required
          />
          <TextField
            label="Inspector Remarks"
            value={inspectorRemarks}
            onChangeText={setInspectorRemarks}
            multiline
            required
          />
        </View>

        <View style={styles.section}>
          <ThemedText type="subtitle">Audit Trail</ThemedText>
          <ThemedText>Inspector: {inspectorName || "—"}</ThemedText>
          <ThemedText>Date & Time: {new Date().toLocaleString()}</ThemedText>
          <ThemedText>GPS: {gps || "—"}</ThemedText>
          <ThemedText>Device ID: {deviceId || "—"}</ThemedText>
        </View>
      </ScrollView>

      <View style={styles.footer}>
        <PrimaryButton
          title="Save Inspection"
          disabled={false}
          onPress={async () => {
            if (!grower) {
              Alert.alert("Missing grower", "Grower must exist in system.");
              return;
            }
            if (!canSubmit) {
              Alert.alert(
                "Missing fields",
                `Complete all mandatory fields before saving:\n\n${missingFields.join("\n")}`,
              );
              return;
            }

            const payload: CuringInspection = {
              numberOfBarns: numberOfBarns.trim(),
              typeOfBarns,
              curingCycles: curingCycles.trim(),
              fuelSource: fuelSource.trim(),
              curingStatus,
              leafQuality,
              gradingStatus,
              inspectorRemarks: inspectorRemarks.trim(),
            };

            const { generateId } = await import("@/lib/inspection-storage");
            const report: InspectionReport = {
              id: params.editingReportId?.toString() || generateId(),
              stage: "Curing",
              grower,
              audit: {
                inspectorName: inspectorName.trim(),
                submittedAt: new Date().toISOString(),
                gps: gps.trim(),
                deviceId,
              },
              payload,
              inspectionId,
              syncStatus: "pending",
              lastError: "",
            };

            await saveInspectionReport(report);
            Alert.alert("Saved", "Saved to Conducted (Pending Sync).", [
              { text: "View Pending Sync", onPress: () => router.replace("/inspection/reports" as any) },
              { text: "Back", onPress: () => router.replace("/inspection" as any) },
            ]);
          }}
        />
      </View>
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
    paddingBottom: 120,
  },
  section: {
    gap: 10,
  },
  footer: {
    padding: 20,
    paddingTop: 0,
  },
});
