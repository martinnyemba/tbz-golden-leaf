import MaterialIcons from "@expo/vector-icons/MaterialIcons";
import { useFocusEffect, useLocalSearchParams, useRouter } from "expo-router";
import { useCallback, useMemo, useState } from "react";
import { Alert, ScrollView, StyleSheet, View } from "react-native";

import { FullScreenLeafLoader } from "@/components/LeafLoader";
import { ThemedText } from "@/components/themed-text";
import { ThemedView } from "@/components/themed-view";
import { PrimaryButton, SelectField, TextField } from "@/components/ui/form-controls";
import { Colors } from "@/constants/theme";
import { useColorScheme } from "@/hooks/use-color-scheme";
import { apiErrorUi, apiFetchJson, saveGrowerEditDraft } from "@/lib/inspection-storage";

const PROVINCES = ["Central", "Copperbelt", "Eastern", "Luapula", "Lusaka", "Muchinga", "Northern", "North-Western", "Southern", "Western"];

function normalizeProvince(value: string) {
  const v = String(value ?? "").trim();
  if (!v) return "";
  const match = PROVINCES.find((p) => p.toUpperCase().replace("-", "_") === v.toUpperCase().replace("-", "_"));
  return match ?? v;
}

export default function GrowerEditScreen() {
  const router = useRouter();
  const theme = (useColorScheme() ?? "light") === "dark" ? "dark" : "light";
  const params = useLocalSearchParams<{ id?: string }>();
  const id = (params.id ?? "").toString();

  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  const [firstName, setFirstName] = useState("");
  const [middleName, setMiddleName] = useState("");
  const [lastName, setLastName] = useState("");
  const [nrcNumber, setNrcNumber] = useState("");
  const [sex, setSex] = useState("");
  const [dateOfBirth, setDateOfBirth] = useState("");
  const [phoneNumber, setPhoneNumber] = useState("");
  const [email, setEmail] = useState("");
  const [address, setAddress] = useState("");
  const [townOrVillage, setTownOrVillage] = useState("");
  const [province, setProvince] = useState("");
  const [district, setDistrict] = useState("");

  const canSave = useMemo(() => {
    if (!id) return false;
    if (!firstName.trim() && !lastName.trim()) return false;
    if (!province.trim()) return false;
    if (!district.trim()) return false;
    return true;
  }, [district, firstName, id, lastName, province]);

  useFocusEffect(
    useCallback(() => {
      let mounted = true;
      (async () => {
        if (!id) {
          setLoading(false);
          return;
        }
        setLoading(true);
        try {
          const resp = await apiFetchJson(`/api/v1/growers/growers/${encodeURIComponent(id)}/`, { method: "GET" });
          if (!mounted) return;
          if (resp.ok) {
            const g = JSON.parse(resp.body) as any;
            setFirstName(String(g?.first_name ?? ""));
            setMiddleName(String(g?.middle_name ?? ""));
            setLastName(String(g?.last_name ?? ""));
            setNrcNumber(String(g?.nrc_number ?? ""));
            setSex(String(g?.sex ?? ""));
            setDateOfBirth(String(g?.date_of_birth ?? ""));
            setPhoneNumber(String(g?.phone_number ?? ""));
            setEmail(String(g?.email ?? ""));
            setAddress(String(g?.address ?? ""));
            setTownOrVillage(String(g?.town_or_village ?? ""));
            setProvince(normalizeProvince(String(g?.province_display ?? g?.province ?? "")));
            setDistrict(String(g?.district ?? ""));
          }
        } catch {
        } finally {
          if (mounted) setLoading(false);
        }
      })();
      return () => {
        mounted = false;
      };
    }, [id]),
  );

  const handleSave = async () => {
    if (!canSave) return;
    setSaving(true);
    try {
      const payload: Record<string, unknown> = {
        first_name: firstName.trim(),
        middle_name: middleName.trim(),
        last_name: lastName.trim(),
        nrc_number: nrcNumber.trim(),
        sex: sex.trim(),
        date_of_birth: dateOfBirth.trim(),
        phone_number: phoneNumber.trim(),
        email: email.trim(),
        address: address.trim(),
        town_or_village: townOrVillage.trim(),
        province: province.trim().toUpperCase().replace(/[-\s]/g, "_"),
        district: district.trim(),
      };

      const resp = await apiFetchJson(`/api/v1/growers/growers/${encodeURIComponent(id)}/`, {
        method: "PATCH",
        body: JSON.stringify(payload),
      });

      if (resp.ok) {
        Alert.alert("Saved", "Grower details updated.", [{ text: "OK", onPress: () => router.back() }]);
        return;
      }

      if (resp.status === 0) {
        await saveGrowerEditDraft({ growerId: id, payload });
        Alert.alert("Saved offline", "Saved pending sync. It will upload when internet is available.", [
          { text: "OK", onPress: () => router.back() },
        ]);
        return;
      }

      const ui = apiErrorUi(resp.status, resp.body);
      if (ui.kind === "login_required") {
        Alert.alert(ui.title, ui.message, [
          {
            text: "Login",
            onPress: () =>
              router.push({
                pathname: "/portal-login",
                params: { returnTo: `/registration/grower-edit?id=${encodeURIComponent(id)}` },
              } as any),
          },
          { text: "OK" },
        ]);
        return;
      }
      Alert.alert(ui.title, ui.message.slice(0, 260));
    } catch (e) {
      const msg = e instanceof Error ? e.message : String(e ?? "Error");
      Alert.alert("Failed", msg.slice(0, 260));
    } finally {
      setSaving(false);
    }
  };

  if (loading) return <FullScreenLeafLoader label="Loading…" />;

  return (
    <ThemedView style={styles.container}>
      <ScrollView contentContainerStyle={styles.content}>
        <View style={[styles.card, { borderColor: Colors[theme].border, backgroundColor: Colors[theme].surface }]}>
          <ThemedText type="defaultSemiBold">Personal</ThemedText>
          <TextField label="First Name" value={firstName} onChangeText={setFirstName} required />
          <TextField label="Middle Name" value={middleName} onChangeText={setMiddleName} />
          <TextField label="Last Name" value={lastName} onChangeText={setLastName} required />
          <TextField label="NRC / PACRA" value={nrcNumber} onChangeText={setNrcNumber} />
          <SelectField label="Sex" value={sex} onChange={setSex} options={["", "MALE", "FEMALE"]} />
          <TextField label="Date of Birth (YYYY-MM-DD)" value={dateOfBirth} onChangeText={setDateOfBirth} />
          <TextField label="Phone Number" value={phoneNumber} onChangeText={setPhoneNumber} keyboardType="phone-pad" />
          <TextField label="Email" value={email} onChangeText={setEmail} keyboardType="email-address" />
        </View>

        <View style={[styles.card, { borderColor: Colors[theme].border, backgroundColor: Colors[theme].surface }]}>
          <ThemedText type="defaultSemiBold">Location</ThemedText>
          <TextField label="Address / Farm" value={address} onChangeText={setAddress} />
          <TextField label="Town / Village" value={townOrVillage} onChangeText={setTownOrVillage} />
          <SelectField label="Province *" value={province} onChange={setProvince} options={PROVINCES} required />
          <TextField label="District *" value={district} onChangeText={setDistrict} required />
        </View>

        <PrimaryButton title={saving ? "Saving…" : "Save Changes"} onPress={handleSave} disabled={!canSave} loading={saving} />
      </ScrollView>
    </ThemedView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  content: { padding: 16, gap: 12, paddingBottom: 40 },
  titleRow: { flexDirection: "row", alignItems: "center", gap: 10, marginBottom: 6 },
  card: { borderWidth: 1, borderRadius: 18, padding: 14, gap: 10, overflow: "hidden" },
});
