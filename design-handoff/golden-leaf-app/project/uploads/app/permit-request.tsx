import { useMemo, useState, useCallback } from "react";
import { Alert, Pressable, StyleSheet, ScrollView, View } from "react-native";
import { useFocusEffect, useRouter } from "expo-router";
import MaterialIcons from "@expo/vector-icons/MaterialIcons";

import { ThemedText } from "@/components/themed-text";
import { ThemedView } from "@/components/themed-view";
import { PrimaryButton, SelectField, TextField } from "@/components/ui/form-controls";
import { LeafLoader } from "@/components/LeafLoader";
import { readCacheItems, writeCache } from "@/lib/offline-cache";
import { apiErrorUi, apiFetchJson, extractErrorMessage, savePendingPermitRequest } from "@/lib/inspection-storage";
import { checkApiReachable } from "@/lib/network-state";
import { FontFamily, FontWeight } from "@/constants/typography";

const GROWER_CATEGORIES = ["Small Scale", "Commercial", "Company"];
const PURPOSES = ["Sales", "Processing", "Storage", "Export"];
const PROVINCES = ["Central", "Copperbelt", "Eastern", "Luapula", "Lusaka", "Muchinga", "Northern", "North-Western", "Southern", "Western"];
const DISTRICTS: Record<string, string[]> = {
  Central: ["Chibombo", "Chisamba", "Chitambo", "Itezhi-Tezhi", "Kabwe", "Kapiri Mposhi", "Luano", "Mkushi", "Mumbwa", "Ngabwe", "Serenje", "Shibuyunji"],
  Copperbelt: ["Chililabombwe", "Chingola", "Kalulushi", "Kitwe", "Luanshya", "Lufwanyama", "Masaiti", "Mpongwe", "Mufulira", "Ndola"],
  Eastern: ["Chadiza", "Chama", "Chasefu", "Chipangali", "Chipata", "Kasenengwa", "Katete", "Lumezi", "Lundazi", "Mambwe", "Nyimba", "Petauke", "Sinda", "Vubwi"],
  Luapula: ["Chembe", "Chienge", "Chifunabuli", "Kawambwa", "Lunga", "Mansa", "Milenge", "Mwansabombwe", "Mwense", "Nchelenge", "Samfya"],
  Lusaka: ["Chilanga", "Chongwe", "Kafue", "Luangwa", "Lusaka", "Rufunsa"],
  Muchinga: ["Chama", "Chinsali", "Isoka", "Kanchibiya", "Lavushimanda", "Mafinga", "Mpika", "Nakonde", "Shiwa Ng'andu"],
  Northern: ["Chilubi", "Kaputa", "Kasama", "Lunte", "Luwingu", "Mbala", "Mporokoso", "Mpulungu", "Mungwi", "Nsama", "Senga Hill"],
  "North-Western": ["Chavuma", "Ikelenge", "Kabompo", "Kalumbila", "Kasempa", "Manyinga", "Mufumbwe", "Mushindamo", "Mwinilunga", "Solwezi", "Zambezi"],
  Southern: ["Chikankata", "Choma", "Gwembe", "Kalomo", "Kazungula", "Livingstone", "Mazabuka", "Monze", "Namwala", "Pemba", "Siavonga", "Sinazongwe", "Zimba"],
  Western: ["Kalabo", "Kaoma", "Limulunga", "Luampa", "Lukulu", "Mitete", "Mongu", "Mulobezi", "Mwandi", "Nalolo", "Nkeyema", "Senanga", "Sesheke", "Shang'ombo", "Sikongo", "Sioma"],
};

function WizardStepper({ step }: { step: 1 | 2 | 3 }) {
  return (
    <View style={styles.stepper}>
      <View style={[styles.stepItem, step === 1 ? styles.stepActive : step > 1 ? styles.stepDone : null]}>
        <View style={[styles.stepIndex, step === 1 ? styles.stepIndexActive : step > 1 ? styles.stepIndexDone : null]}>
          {step > 1 ? <MaterialIcons name="check" size={14} color="#FFF" /> : <ThemedText style={styles.stepIndexText} lightColor="#FFF" darkColor="#FFF">1</ThemedText>}
        </View>
        <ThemedText style={styles.stepLabelText}>Grower</ThemedText>
      </View>
      <View style={[styles.stepItem, step === 2 ? styles.stepActive : step > 2 ? styles.stepDone : null]}>
        <View style={[styles.stepIndex, step === 2 ? styles.stepIndexActive : step > 2 ? styles.stepIndexDone : null]}>
          {step > 2 ? <MaterialIcons name="check" size={14} color="#FFF" /> : <ThemedText style={styles.stepIndexText} lightColor="#FFF" darkColor="#FFF">2</ThemedText>}
        </View>
        <ThemedText style={styles.stepLabelText}>Transport</ThemedText>
      </View>
      <View style={[styles.stepItem, step === 3 ? styles.stepActive : null]}>
        <View style={[styles.stepIndex, step === 3 ? styles.stepIndexActive : null]}>
          <ThemedText style={styles.stepIndexText} lightColor="#FFF" darkColor="#FFF">3</ThemedText>
        </View>
        <ThemedText style={styles.stepLabelText}>Buyer</ThemedText>
      </View>
    </View>
  );
}

function ProgressBar({ step }: { step: 1 | 2 | 3 }) {
  return (
    <View style={styles.progressTrack}>
      <View style={[styles.progressFill, { width: step === 1 ? "33%" : step === 2 ? "66%" : "100%" }]} />
    </View>
  );
}

function formatProvinceForUI(p: string) {
  if (!p) return "";
  const match = PROVINCES.find(x => x.toUpperCase().replace("-", "_") === p.toUpperCase().replace("-", "_"));
  return match || p;
}

export default function PermitRequestScreen() {
  const router = useRouter();
  
  const [growerId, setGrowerId] = useState("");
  const [selectedGrowerName, setSelectedGrowerName] = useState("");
  const [selectedGrowerTbzId, setSelectedGrowerTbzId] = useState("");
  const [selectedGrowerNrc, setSelectedGrowerNrc] = useState("");
  
  const [searchQuery, setSearchQuery] = useState("");
  const [searchResults, setSearchResults] = useState<any[]>([]);
  const [isSearching, setIsSearching] = useState(false);
  
  const [growerCategory, setGrowerCategory] = useState("");
  const [totalBales, setTotalBales] = useState("");
  const [totalWeightKg, setTotalWeightKg] = useState("");
  const [licensePlate, setLicensePlate] = useState("");
  const [province, setProvince] = useState("");
  const [district, setDistrict] = useState("");
  const [destinationSalesfloor, setDestinationSalesfloor] = useState("");
  const [purpose, setPurpose] = useState("");
  const [isBought, setIsBought] = useState(false);
  const [buyer, setBuyer] = useState("");
  const [buyerAccepted, setBuyerAccepted] = useState(false);
  const [comments, setComments] = useState("");
  
  const [step, setStep] = useState<1 | 2 | 3>(1);
  const [salesfloors, setSalesfloors] = useState<{ id: string, name: string }[]>([]);
  const [buyersList, setBuyersList] = useState<{ id: string, name: string }[]>([]);
  const [optionsLoading, setOptionsLoading] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const districts = useMemo(() => (province ? DISTRICTS[province] ?? [] : []), [province]);
  const sfOptions = useMemo(
    () =>
      salesfloors
        .map((sf: any) => String(sf?.name ?? sf?.salesfloor_name ?? sf?.legal_name ?? sf?.company_name ?? "").trim())
        .filter(Boolean),
    [salesfloors],
  );
  const buyerOptions = useMemo(
    () =>
      ["None", ...buyersList.map((b: any) => String(b?.name ?? b?.company_name ?? "").trim()).filter(Boolean)],
    [buyersList],
  );

  useFocusEffect(
    useCallback(() => {
      async function fetchSalesfloors() {
        setOptionsLoading(true);
        try {
          const cachedSf = await readCacheItems<any>("salesfloors");
          if (cachedSf.length > 0 && salesfloors.length === 0) setSalesfloors(cachedSf);
          const cachedBuyers = await readCacheItems<any>("buyers");
          if (cachedBuyers.length > 0 && buyersList.length === 0) setBuyersList(cachedBuyers);

          const resp = await apiFetchJson("/api/v1/entities/legal-entities/?entity_type=SALES_FLOOR", { method: "GET" });
          if (resp.ok) {
            const data = JSON.parse(resp.body);
            const rows = Array.isArray(data) ? data : (data.results ?? []);
            setSalesfloors(rows);
            if (Array.isArray(rows) && rows.length > 0) void writeCache("salesfloors", rows);
          }
          const respBuyer = await apiFetchJson("/api/v1/entities/legal-entities/?entity_type=BUYER", { method: "GET" });
          if (respBuyer.ok) {
            const dataB = JSON.parse(respBuyer.body);
            const rowsB = Array.isArray(dataB) ? dataB : (dataB.results ?? []);
            setBuyersList(rowsB);
            if (Array.isArray(rowsB) && rowsB.length > 0) void writeCache("buyers", rowsB);
          }
        } catch {
        } finally {
          setOptionsLoading(false);
        }
      }
      fetchSalesfloors();
    }, [])
  );

  const searchGrowers = async () => {
    if (!searchQuery.trim()) return;
    setIsSearching(true);
    try {
      const resp = await apiFetchJson(`/api/v1/growers/growers/?search=${encodeURIComponent(searchQuery.trim())}`, { method: "GET" });
      if (resp.ok) {
        const data = JSON.parse(resp.body);
        setSearchResults(Array.isArray(data) ? data : (data.results ?? []));
      }
    } catch (e) {
      console.log("Search error:", e);
    } finally {
      setIsSearching(false);
    }
  };

  const selectGrower = (g: any) => {
    setGrowerId(g.id);
    setSelectedGrowerName(g.display_name);
    setSelectedGrowerTbzId(g.tbz_id || "");
    setSelectedGrowerNrc(g.nrc_number || "");
    setProvince(formatProvinceForUI(g.province || ""));
    setDistrict(g.district || "");
    setSearchResults([]);
    setSearchQuery("");
  };

  const validateStep1 = () => {
    const missing: string[] = [];
    if (!growerId.trim()) missing.push("Grower ID");
    if (!growerCategory) missing.push("Grower Category");
    if (missing.length > 0) {
      Alert.alert("Missing Information", `Please complete:\n${missing.join("\n")}`);
      return false;
    }
    return true;
  };

  const validateStep2 = () => {
    const missing: string[] = [];
    if (!totalBales.trim()) missing.push("Total Bales");
    if (!totalWeightKg.trim()) missing.push("Total Weight (Kg)");
    if (!purpose) missing.push("Purpose");
    if (!licensePlate.trim()) missing.push("License Plate");
    if (!province) missing.push("Origin Province");
    if (!district) missing.push("Origin District");
    if (!destinationSalesfloor.trim()) missing.push("Destination Salesfloor");
    if (missing.length > 0) {
      Alert.alert("Missing Information", `Please complete:\n${missing.join("\n")}`);
      return false;
    }
    return true;
  };

  const resetForm = () => {
    setStep(1);
    setGrowerId("");
    setSelectedGrowerName("");
    setSelectedGrowerTbzId("");
    setSelectedGrowerNrc("");
    setSearchQuery("");
    setSearchResults([]);
    setGrowerCategory("");
    setTotalBales("");
    setTotalWeightKg("");
    setLicensePlate("");
    setProvince("");
    setDistrict("");
    setDestinationSalesfloor("");
    setPurpose("");
    setIsBought(false);
    setBuyer("");
    setBuyerAccepted(false);
    setComments("");
  };

  const handleSubmit = async () => {
    if (!validateStep1() || !validateStep2()) return;

    setIsSubmitting(true);
    try {
      const catMap: Record<string, string> = {
        "Small Scale": "SMALL_SCALE",
        "Commercial": "COMMERCIAL",
        "Company": "COMPANY"
      };
      const purposeMap: Record<string, string> = {
        "Sales": "SALES",
        "Processing": "PROCESSING",
        "Storage": "STORAGE",
        "Export": "EXPORT"
      };

      const selectedBuyerId = buyer === "None" ? null : buyersList.find(b => b.name === buyer)?.id || null;

      const payload = {
        grower: growerId.trim(),
        grower_category: catMap[growerCategory],
        total_bales: parseInt(totalBales.trim(), 10),
        total_weight_kg: parseFloat(totalWeightKg.trim()),
        license_plate: licensePlate.trim(),
        origin_province: province.toUpperCase().replace("-", "_"),
        origin_district: district,
        destination_salesfloor: destinationSalesfloor.trim(),
        purpose: purposeMap[purpose] || "SALES",
        is_bought: isBought,
        buyer: selectedBuyerId,
        buyer_accepted: buyerAccepted,
        comments: comments.trim()
      };

      const queueForLater = async (message: string) => {
        await savePendingPermitRequest(payload, {
          growerName: selectedGrowerName,
          growerTbzId: selectedGrowerTbzId,
          destinationSalesfloor: destinationSalesfloor.trim(),
          totalBales: parseInt(totalBales.trim(), 10),
        });
        resetForm();
        Alert.alert("Saved Offline", message, [
          { text: "OK", onPress: () => router.back() }
        ]);
      };

      const canReachApi = await checkApiReachable();
      if (!canReachApi) {
        await queueForLater(
          "Transport permit request saved locally. It will be submitted automatically when the device reconnects."
        );
        return;
      }

      // 1. Pre-flight check: Verify grower has an APPROVED validation to prevent a backend 500 crash
      const valResp = await apiFetchJson(`/api/v1/inspectorate/validations/?grower=${growerId.trim()}`, { method: "GET" });
      if (valResp.ok) {
        const valData = JSON.parse(valResp.body);
        const validations = Array.isArray(valData) ? valData : (valData.results ?? []);
        
        const hasApproved = validations.some((v: any) => v.validation_status === "APPROVED");
        const hasPending = validations.some((v: any) => v.validation_status === "PENDING");
        const hasRejected = validations.some((v: any) => v.validation_status === "REJECTED");
        
        if (!hasApproved) {
          setIsSubmitting(false);
          if (hasPending) {
            Alert.alert(
              "Validation Pending",
              "This grower has a validation inspection submitted that is currently PENDING. It must be finalized before a transit permit can be issued."
            );
          } else if (hasRejected) {
            Alert.alert(
              "Validation Rejected",
              "This grower has a validation that was REJECTED. A new validation must be completed and approved before a transit permit can be issued.",
              [
                { text: "Cancel", style: "cancel" },
                {
                  text: "Validate Now",
                  style: "default",
                  onPress: () => {
                    router.push({
                      pathname: "/validation",
                      params: {
                        grower: JSON.stringify({
                          portalGrowerId: growerId,
                          name: selectedGrowerName,
                          growerId: selectedGrowerTbzId,
                          nrc: selectedGrowerNrc,
                          province: province,
                          district: district
                        })
                      }
                    });
                  }
                }
              ]
            );
          } else {
            Alert.alert(
              "Validation Required",
              "This grower does not have an approved validation. You must complete a validation inspection before issuing a transit permit.",
              [
                { text: "Cancel", style: "cancel" },
                { 
                  text: "Validate Now", 
                  style: "default",
                  onPress: () => {
                    router.push({
                      pathname: "/validation",
                      params: {
                        grower: JSON.stringify({
                          portalGrowerId: growerId,
                          name: selectedGrowerName,
                          growerId: selectedGrowerTbzId,
                          nrc: selectedGrowerNrc,
                          province: province,
                          district: district
                        })
                      }
                    });
                  }
                }
              ]
            );
          }
          return;
        }
      }

      // 2. Submit actual transit permit
      const resp = await apiFetchJson("/api/v1/permits/transport-permits/", {
        method: "POST",
        body: JSON.stringify(payload)
      });

      if (resp.ok) {
        resetForm();
        Alert.alert("Success", "Transport permit application submitted successfully.", [
          { text: "OK", onPress: () => router.back() }
        ]);
      } else {
        const ui = apiErrorUi(resp.status, resp.body);
        if (ui.kind === "network_error") {
          await queueForLater(
            "Network unavailable. Transport permit request saved locally and will sync automatically when you reconnect."
          );
        } else if (resp.status === 500) {
          Alert.alert("Server Error", "The server encountered an error processing your request. Please ensure the grower is fully active and validated.");
        } else {
          const errorMsg = extractErrorMessage(resp.body);
          Alert.alert("Failure", `Error submitting permit: ${errorMsg}`);
        }
      }
    } catch (e: any) {
      const fallback = e?.message ? String(e.message) : "Network error";
      const catMap: Record<string, string> = {
        "Small Scale": "SMALL_SCALE",
        "Commercial": "COMMERCIAL",
        "Company": "COMPANY"
      };
      const purposeMap: Record<string, string> = {
        "Sales": "SALES",
        "Processing": "PROCESSING",
        "Storage": "STORAGE",
        "Export": "EXPORT"
      };
      await savePendingPermitRequest(
        {
          grower: growerId.trim(),
          grower_category: catMap[growerCategory],
          total_bales: parseInt(totalBales.trim(), 10),
          total_weight_kg: parseFloat(totalWeightKg.trim()),
          license_plate: licensePlate.trim(),
          origin_province: province.toUpperCase().replace("-", "_"),
          origin_district: district,
          destination_salesfloor: destinationSalesfloor.trim(),
          purpose: purposeMap[purpose] || "SALES",
          is_bought: isBought,
          buyer: buyer === "None" ? null : buyersList.find((b) => b.name === buyer)?.id || null,
          buyer_accepted: buyerAccepted,
          comments: comments.trim(),
        },
        {
          growerName: selectedGrowerName,
          growerTbzId: selectedGrowerTbzId,
          destinationSalesfloor: destinationSalesfloor.trim(),
          totalBales: parseInt(totalBales.trim(), 10),
        },
      );
      resetForm();
      Alert.alert("Saved Offline", `Unable to reach the server right now. ${fallback}. The permit request was queued for sync.`, [
        { text: "OK", onPress: () => router.back() }
      ]);
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <ThemedView style={styles.container}>
      <ScrollView contentContainerStyle={styles.content}>
        <WizardStepper step={step} />
        <ProgressBar step={step} />

        <View style={styles.card}>
          {step === 1 && (
            <View>
              <View style={styles.cardHeader}>
                <ThemedText type="defaultSemiBold" style={styles.cardTitle}>Step 1 — Grower Information</ThemedText>
                <ThemedText style={styles.cardSubtitle} lightColor="#6B7280" darkColor="#9CA3AF">Select the grower for this permit</ThemedText>
              </View>
              <View style={styles.cardBody}>
                {selectedGrowerName ? (
                  <View style={{ flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', backgroundColor: '#F3F4F6', padding: 12, borderRadius: 8, marginBottom: 16 }}>
                    <View>
                      <ThemedText style={{ fontSize: 13, color: '#6B7280' }}>Selected Grower</ThemedText>
                      <ThemedText style={{ fontSize: 15, fontFamily: FontFamily.serif, fontWeight: FontWeight.medium, color: '#111827' }}>
                        {selectedGrowerName}
                      </ThemedText>
                    </View>
                    <Pressable onPress={() => { setGrowerId(""); setSelectedGrowerName(""); setProvince(""); setDistrict(""); }} style={{ padding: 4 }}>
                      <MaterialIcons name="close" size={20} color="#6B7280" />
                    </Pressable>
                  </View>
                ) : (
                  <View style={{ marginBottom: 16 }}>
                    <View style={{ flexDirection: 'row', gap: 8, alignItems: 'center' }}>
                       <View style={{ flex: 1 }}>
                         <TextField label="Grower *" value={searchQuery} onChangeText={setSearchQuery} placeholder="Search name, ID, or NRC..." />
                       </View>
                       <Pressable 
                         style={{ backgroundColor: '#0B6B3A', justifyContent: 'center', alignItems: 'center', width: 44, height: 44, borderRadius: 8, marginTop: 14 }} 
                         onPress={searchGrowers}
                       >
                          {isSearching ? <LeafLoader size={18} color="#FFF" /> : <MaterialIcons name="search" color="#FFF" size={22} />}
                       </Pressable>
                    </View>
                    
                    {searchResults.length > 0 && (
                      <View style={{ borderWidth: 1, borderColor: '#E5E7EB', borderRadius: 8, marginTop: 8, maxHeight: 220, overflow: 'hidden' }}>
                        <ScrollView nestedScrollEnabled keyboardShouldPersistTaps="handled">
                          {searchResults.map(g => (
                            <Pressable 
                              key={g.id} 
                              style={({pressed}) => ({ padding: 12, borderBottomWidth: 1, borderBottomColor: '#F3F4F6', backgroundColor: pressed ? '#F9FAFB' : '#FFF' })} 
                              onPress={() => selectGrower(g)}
                            >
                              <ThemedText style={{ fontFamily: FontFamily.serif, fontWeight: FontWeight.medium, color: '#111827' }}>{g.display_name}</ThemedText>
                              <ThemedText style={{ fontSize: 13, color: '#6B7280', marginTop: 2 }}>{g.tbz_id}  •  {g.nrc_number || "No NRC"}</ThemedText>
                            </Pressable>
                          ))}
                        </ScrollView>
                      </View>
                    )}
                  </View>
                )}

                <SelectField label="Grower Category *" value={growerCategory} onChange={setGrowerCategory} options={GROWER_CATEGORIES} required />
                <View style={{ flexDirection: 'row', gap: 12, marginTop: 4 }}>
                  <View style={{ flex: 1 }}>
                    <TextField label="Province (auto-filled)" value={province} onChangeText={() => {}} placeholder="Read-only" editable={false} />
                  </View>
                  <View style={{ flex: 1 }}>
                    <TextField label="District (auto-filled)" value={district} onChangeText={() => {}} placeholder="Read-only" editable={false} />
                  </View>
                </View>

                <View style={styles.actions}>
                  <Pressable style={[styles.btn, styles.btnSecondary]} onPress={() => router.back()}>
                    <MaterialIcons name="arrow-back" size={18} color="#111827" />
                    <ThemedText type="defaultSemiBold">Cancel</ThemedText>
                  </Pressable>
                  <Pressable style={[styles.btn, styles.btnPrimary]} onPress={() => validateStep1() && setStep(2)}>
                    <ThemedText type="defaultSemiBold" lightColor="#FFFFFF" darkColor="#FFFFFF">Continue</ThemedText>
                    <MaterialIcons name="arrow-forward" size={18} color="#FFFFFF" />
                  </Pressable>
                </View>
              </View>
            </View>
          )}

          {step === 2 && (
            <View>
              <View style={styles.cardHeader}>
                <ThemedText type="defaultSemiBold" style={styles.cardTitle}>Step 2 — Tobacco & Transport Details</ThemedText>
                <ThemedText style={styles.cardSubtitle} lightColor="#6B7280" darkColor="#9CA3AF">Enter tobacco and transport details</ThemedText>
              </View>
              <View style={styles.cardBody}>
                <ThemedText type="defaultSemiBold" style={styles.sectionTitle}>Tobacco Details</ThemedText>
                <View style={{ flexDirection: 'row', gap: 12 }}>
                  <View style={{ flex: 1 }}><TextField label="Total Bales *" value={totalBales} onChangeText={setTotalBales} keyboardType="number-pad" required /></View>
                  <View style={{ flex: 1 }}><TextField label="Weight (kg) *" value={totalWeightKg} onChangeText={setTotalWeightKg} keyboardType="decimal-pad" required /></View>
                </View>
                <SelectField label="Purpose *" value={purpose} onChange={setPurpose} options={PURPOSES} required />
                
                <Pressable style={styles.checkboxOption} onPress={() => setIsBought(!isBought)}>
                  <MaterialIcons name={isBought ? "check-box" : "check-box-outline-blank"} size={22} color={isBought ? "#0B6B3A" : "#9CA3AF"} />
                  <View style={{ flex: 1 }}>
                    <ThemedText style={{ fontFamily: FontFamily.serif, fontWeight: FontWeight.medium, fontSize: 14 }}>Tobacco already bought</ThemedText>
                    <ThemedText style={{ fontSize: 12, color: '#6B7280' }}>Check this if the tobacco being transported is pre-sold before reaching the sales floor</ThemedText>
                  </View>
                </Pressable>

                <View style={[styles.divider, { marginVertical: 8 }]} />
                <ThemedText type="defaultSemiBold" style={styles.sectionTitle}>Transport Details</ThemedText>
                <TextField label="Vehicle License Plate *" value={licensePlate} onChangeText={setLicensePlate} placeholder="e.g. ALH 1234 ZM" required />
                <SelectField label="Origin Province *" value={province} onChange={setProvince} options={PROVINCES} required />
                <SelectField label="Origin District *" value={district} onChange={setDistrict} options={districts} disabled={!province} required />
                {sfOptions.length > 0 ? (
                  <SelectField
                    label="Destination Sales Floor *"
                    value={destinationSalesfloor}
                    onChange={setDestinationSalesfloor}
                    options={sfOptions}
                    placeholder={optionsLoading ? "Loading…" : "Select…"}
                    required
                    disabled={optionsLoading}
                  />
                ) : (
                  <TextField
                    label="Destination Sales Floor *"
                    value={destinationSalesfloor}
                    onChangeText={setDestinationSalesfloor}
                    placeholder={optionsLoading ? "Loading…" : "Type sales floor name"}
                    required
                  />
                )}
                
                <View style={styles.infoBanner}>
                    <MaterialIcons name="calendar-today" size={16} color="#0B6B3A" />
                    <ThemedText style={{ fontSize: 12, marginLeft: 6 }}>Validity dates (Valid From / Valid To) are set by the inspector upon approval.</ThemedText>
                </View>

                <View style={styles.actions}>
                  <Pressable style={[styles.btn, styles.btnSecondary]} onPress={() => setStep(1)}>
                    <MaterialIcons name="arrow-back" size={18} color="#111827" />
                    <ThemedText type="defaultSemiBold">Back</ThemedText>
                  </Pressable>
                  <Pressable style={[styles.btn, styles.btnPrimary]} onPress={() => validateStep2() && setStep(3)}>
                    <ThemedText type="defaultSemiBold" lightColor="#FFFFFF" darkColor="#FFFFFF">Continue</ThemedText>
                    <MaterialIcons name="arrow-forward" size={18} color="#FFFFFF" />
                  </Pressable>
                </View>
              </View>
            </View>
          )}

          {step === 3 && (
            <View>
              <View style={styles.cardHeader}>
                <ThemedText type="defaultSemiBold" style={styles.cardTitle}>Step 3 — Buyer Information & Notes</ThemedText>
                <ThemedText style={styles.cardSubtitle} lightColor="#6B7280" darkColor="#9CA3AF">Assign buyer and add any notes (for record purposes only)</ThemedText>
              </View>
              <View style={styles.cardBody}>
                <SelectField label="Assigned Buyer" value={buyer} onChange={setBuyer} options={buyerOptions} />
                
                <Pressable style={styles.checkboxOption} onPress={() => setBuyerAccepted(!buyerAccepted)}>
                  <MaterialIcons name={buyerAccepted ? "check-box" : "check-box-outline-blank"} size={22} color={buyerAccepted ? "#0B6B3A" : "#9CA3AF"} />
                  <View style={{ flex: 1 }}>
                    <ThemedText style={{ fontFamily: FontFamily.serif, fontWeight: FontWeight.medium, fontSize: 14 }}>Buyer has accepted / confirmed purchase</ThemedText>
                    <ThemedText style={{ fontSize: 12, color: '#6B7280' }}>Tick this only if the buyer has formally agreed to purchase this tobacco before the permit is issued</ThemedText>
                  </View>
                </Pressable>

                <View style={[styles.divider, { marginVertical: 8 }]} />
                <ThemedText type="defaultSemiBold" style={styles.sectionTitle}>Additional Comments</ThemedText>
                <TextField label="Comments" value={comments} onChangeText={setComments} multiline />

                <View style={{ backgroundColor: '#E8F3EE', padding: 12, borderRadius: 8, marginTop: 8, flexDirection: 'row', alignItems: 'center' }}>
                    <MaterialIcons name="verified-user" size={18} color="#0B6B3A" style={{ marginRight: 8 }} />
                    <ThemedText style={{ fontSize: 13 }}>Submitted by: <ThemedText type="defaultSemiBold" style={{ fontSize: 13 }}>Current User</ThemedText> | Date: <ThemedText type="defaultSemiBold" style={{ fontSize: 13 }}>{new Date().toLocaleString()}</ThemedText></ThemedText>
                </View>

                <View style={styles.actions}>
                  <Pressable style={[styles.btn, styles.btnSecondary]} onPress={() => setStep(2)}>
                    <MaterialIcons name="arrow-back" size={18} color="#111827" />
                    <ThemedText type="defaultSemiBold">Back</ThemedText>
                  </Pressable>
                  <Pressable style={[styles.btn, styles.btnPrimary, { backgroundColor: '#0B6B3A', borderColor: '#0B6B3A' }]} onPress={handleSubmit} disabled={isSubmitting}>
                    {isSubmitting ? <LeafLoader size={18} color="#FFF" /> : (
                      <>
                        <MaterialIcons name="send" size={18} color="#FFF" />
                        <ThemedText type="defaultSemiBold" lightColor="#FFFFFF" darkColor="#FFFFFF">Submit</ThemedText>
                      </>
                    )}
                  </Pressable>
                </View>
              </View>
            </View>
          )}
        </View>
      </ScrollView>
    </ThemedView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  content: { padding: 16, gap: 12, paddingBottom: 40 },
  header: { gap: 6, marginBottom: 8 },
  headerTitleRow: { flexDirection: "row", alignItems: "center", gap: 8 },
  headerTitle: { fontSize: 20 },
  headerSubtitle: { lineHeight: 18 },
  stepper: { flexDirection: "row", justifyContent: "space-between", gap: 10, paddingVertical: 8 },
  stepItem: { flex: 1, flexDirection: "row", alignItems: "center", gap: 8, padding: 8, borderRadius: 14, borderWidth: 1, borderColor: "#E5E7EB" },
  stepActive: { borderColor: "#0B6B3A", backgroundColor: "#E8F3EE" },
  stepDone: { borderColor: "#0B6B3A" },
  stepIndex: { width: 22, height: 22, borderRadius: 11, alignItems: "center", justifyContent: "center", backgroundColor: "#9CA3AF" },
  stepIndexActive: { backgroundColor: "#0B6B3A" },
  stepIndexDone: { backgroundColor: "#0B6B3A" },
  stepIndexText: { fontSize: 11 },
  stepLabelText: { fontSize: 12, fontFamily: FontFamily.serif, fontWeight: FontWeight.medium, color: '#111827' },
  progressTrack: { height: 6, borderRadius: 999, backgroundColor: "#E5E7EB", overflow: "hidden", marginBottom: 12 },
  progressFill: { height: "100%", backgroundColor: "#0B6B3A" },
  card: { borderWidth: 1, borderColor: "#E5E7EB", borderRadius: 16, overflow: "hidden", backgroundColor: '#FFFFFF' },
  cardHeader: { padding: 14, backgroundColor: "#F9FAFB", gap: 4, borderBottomWidth: 1, borderBottomColor: "#E5E7EB" },
  cardTitle: { fontSize: 16 },
  cardSubtitle: { fontSize: 12, lineHeight: 16 },
  cardBody: { padding: 16, gap: 12 },
  sectionTitle: { fontSize: 14, color: '#0B6B3A', marginBottom: 4 },
  divider: { height: 1, backgroundColor: "#E5E7EB" },
  checkboxOption: { flexDirection: 'row', alignItems: 'flex-start', gap: 10, paddingVertical: 8, paddingHorizontal: 12, backgroundColor: '#F9FAFB', borderRadius: 8, borderWidth: 1, borderColor: '#E5E7EB' },
  infoBanner: { flexDirection: "row", alignItems: "center", backgroundColor: "#E8F3EE", borderRadius: 12, paddingHorizontal: 12, paddingVertical: 10, marginTop: 4 },
  actions: { flexDirection: "row", gap: 12, marginTop: 12 },
  btn: { flex: 1, flexDirection: "row", alignItems: "center", justifyContent: "center", gap: 8, borderRadius: 14, paddingVertical: 12, borderWidth: 1 },
  btnSecondary: { backgroundColor: "transparent", borderColor: "#D1D5DB" },
  btnPrimary: { backgroundColor: "#0B6B3A", borderColor: "#0B6B3A" },
});
