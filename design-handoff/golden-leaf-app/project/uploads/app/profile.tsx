import MaterialIcons from "@expo/vector-icons/MaterialIcons";
import AsyncStorage from "@react-native-async-storage/async-storage";
import { useFocusEffect, useRouter } from "expo-router";
import React, { useCallback, useState } from "react";
import { Alert, Appearance, Pressable, ScrollView, Share, StyleSheet, Switch, View } from "react-native";

import { LeafLoader } from "@/components/LeafLoader";
import { ThemedText } from "@/components/themed-text";
import { ThemedView } from "@/components/themed-view";
import { PrimaryButton } from "@/components/ui/form-controls";
import { Colors } from "@/constants/theme";
import { FontFamily, FontWeight } from "@/constants/typography";
import { useColorScheme } from "@/hooks/use-color-scheme";
import { apiErrorUi, apiFetchJson, AUTH_ACCESS_KEY, AUTH_REFRESH_KEY, unregisterDeviceToken } from "@/lib/inspection-storage";

type UserProfile = {
  id: string;
  email: string;
  first_name: string;
  last_name: string;
  full_name: string;
  phone_number: string;
  role: string;
  province: string;
  district: string;
  is_verified: boolean;
};

export default function ProfileScreen() {
  const theme = (useColorScheme() ?? "light") === "dark" ? "dark" : "light";
  const router = useRouter();

  const [loading, setLoading] = useState(true);
  const [profile, setProfile] = useState<UserProfile | null>(null);
  const [isDarkMode, setIsDarkMode] = useState(theme === "dark");

  const fetchProfile = useCallback(async () => {
    setLoading(true);
    try {
      const resp = await apiFetchJson("/api/v1/auth/me/", { method: "GET" });
      if (resp.ok) {
        try {
          const data = JSON.parse(resp.body) as UserProfile;
          setProfile(data);
        } catch {
          Alert.alert("Error", "Failed to read profile details.");
        }
      } else {
        const ui = apiErrorUi(resp.status, resp.body);
        if (ui.kind === "login_required") {
          Alert.alert(ui.title, ui.message, [
            { text: "Login", onPress: () => router.push({ pathname: "/portal-login", params: { returnTo: "/profile" } } as any) },
            { text: "OK" },
          ]);
        } else {
          Alert.alert(ui.title, ui.message);
        }
      }
    } catch (error) {
      const msg = error instanceof Error ? error.message : "Network request failed.";
      Alert.alert("Network error", msg);
    } finally {
      setLoading(false);
    }
  }, [router]);

  useFocusEffect(
    useCallback(() => {
      fetchProfile();
    }, [fetchProfile])
  );

  const handleLogout = async () => {
    Alert.alert("Log Out", "Are you sure you want to log out?", [
      { text: "Cancel", style: "cancel" },
      {
        text: "Log Out",
        style: "destructive",
        onPress: async () => {
          try {
            try {
              const candidateKeys = ["tbz:push_token", "tbz:expo_push_token", "expoPushToken"];
              for (const k of candidateKeys) {
                const t = await AsyncStorage.getItem(k);
                if (t) {
                  try {
                    await unregisterDeviceToken(t);
                  } catch {}
                  break;
                }
              }
            } catch {}

            try {
              await AsyncStorage.multiRemove([AUTH_ACCESS_KEY, AUTH_REFRESH_KEY]);
            } catch {}

            setProfile(null);
            router.replace("/portal-login");
          } catch {
            setProfile(null);
            router.replace("/portal-login");
          }
        },
      },
    ]);
  };

  const MenuItem = ({ icon, title, subtitle, rightElement, onPress, isDestructive }: any) => (
    <Pressable onPress={onPress} style={styles.menuItem}>
      <View style={styles.menuItemLeft}>
        <MaterialIcons name={icon} size={28} color={isDestructive ? "#DC2626" : Colors[theme].text} />
        <View style={styles.menuItemLabel}>
          <ThemedText style={[styles.menuItemTitle, isDestructive && { color: "#DC2626" }]}>{title}</ThemedText>
          {subtitle && <ThemedText style={styles.menuItemSubtitle}>{subtitle}</ThemedText>}
        </View>
      </View>
      <View style={styles.menuItemRight}>
        {rightElement !== undefined ? rightElement : <MaterialIcons name="chevron-right" size={28} color={Colors[theme].muted} />}
      </View>
    </Pressable>
  );

  return (
    <ThemedView style={styles.container}>
      <ScrollView contentContainerStyle={styles.scrollContent}>
        {loading ? (
          <View style={styles.centerBox}>
            <LeafLoader size={42} />
            <ThemedText style={{ marginTop: 12 }}>Loading profile...</ThemedText>
          </View>
        ) : profile ? (
          <View style={styles.content}>
            <View style={styles.profileSection}>
              <View style={styles.avatarWrapper}>
                <View style={[styles.avatarCircle, { backgroundColor: Colors[theme].background }]}>
                  <ThemedText style={styles.avatarText}>{profile.first_name?.[0] || ""}{profile.last_name?.[0] || ""}</ThemedText>
                </View>
                <View style={[styles.editBadge, { backgroundColor: Colors[theme].primary, borderColor: Colors[theme].surface }]}>
                  <MaterialIcons name="edit" size={14} color={Colors[theme].surface} />
                </View>
              </View>
              <ThemedText style={styles.nameText}>{profile.full_name || "Unknown User"}</ThemedText>
              <ThemedText style={styles.emailText}>{profile.email || "No email provided"}</ThemedText>
            </View>

            <View style={[styles.sectionHeader, { backgroundColor: Colors[theme].background }]}>
              <ThemedText style={styles.sectionHeaderText}>General Settings</ThemedText>
            </View>
            <View style={[styles.sectionBody, { backgroundColor: Colors[theme].surface }]}>
              <MenuItem
                icon={isDarkMode ? "dark-mode" : "brightness-4"}
                title="Mode"
                subtitle="Dark & Light"
                rightElement={
                  <Switch
                    trackColor={{ false: "#E5E7EB", true: Colors[theme].primary }}
                    thumbColor={Colors[theme].surface}
                    value={isDarkMode}
                    onValueChange={async (val: boolean) => {
                      setIsDarkMode(val);
                      Appearance.setColorScheme(val ? "dark" : "light");
                      await AsyncStorage.setItem("tbz:theme", val ? "dark" : "light");
                    }}
                  />
                }
              />
              <MenuItem icon="vpn-key" title="Change Password" onPress={() => router.push('/change-password')} />
              <MenuItem icon="security" title="2F Auth" onPress={() => router.push('/two-factor-auth')} />
            </View>

            <View style={[styles.sectionHeader, { backgroundColor: Colors[theme].background }]}>
              <ThemedText style={styles.sectionHeaderText}>Information</ThemedText>
            </View>
            <View style={[styles.sectionBody, { backgroundColor: Colors[theme].surface }]}>
              <MenuItem icon="smartphone" title="About App" onPress={() => router.push('/about')} />
              <MenuItem icon="description" title="Terms & Conditions" onPress={() => router.push('/terms')} />
              <MenuItem icon="privacy-tip" title="Privacy Policy" onPress={() => router.push('/privacy')} />
              <MenuItem icon="share" title="Share This App" onPress={() => Share.share({ message: 'Check out the Golden leaf mobile app!' })} />
            </View>

            <View style={[styles.sectionHeader, { backgroundColor: Colors[theme].background }]} />
            <View style={[styles.sectionBody, { backgroundColor: Colors[theme].surface, borderBottomWidth: 0 }]}>
              <MenuItem icon="logout" title="Log Out" onPress={handleLogout} isDestructive />
            </View>
          </View>
        ) : (
          <View style={styles.emptyWrap}>
            <View style={[styles.emptyCard, { backgroundColor: Colors[theme].surface }]}>
              <View style={[styles.emptyIconCircle, { backgroundColor: Colors[theme].background }]}>
                <MaterialIcons name="account-circle" size={54} color={Colors[theme].primary} />
              </View>
              <ThemedText style={[styles.emptyTitle, { color: Colors[theme].text }]} numberOfLines={2} ellipsizeMode="tail">
                You&apos;re not logged in
              </ThemedText>
              <ThemedText style={[styles.emptySubtitle, { color: Colors[theme].muted }]} numberOfLines={3} ellipsizeMode="tail">
                Log in to the portal to view your profile and manage your account.
              </ThemedText>
              <View style={styles.emptyCta}>
                <PrimaryButton
                  title="Log In to Portal"
                  onPress={() => router.push({ pathname: "/portal-login", params: { returnTo: "/profile" } } as any)}
                />
              </View>
            </View>
          </View>
        )}
      </ScrollView>
    </ThemedView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#FFFFFF',
  },
  scrollContent: {
    flexGrow: 1,
    paddingBottom: 40,
    backgroundColor: '#FFFFFF',
  },
  centerBox: {
    flex: 1,
    justifyContent: "center",
    alignItems: "center",
    gap: 16,
  },
  emptyWrap: {
    flex: 1,
    alignItems: "center",
    justifyContent: "center",
    paddingHorizontal: 20,
    paddingVertical: 28,
  },
  emptyCard: {
    width: "100%",
    maxWidth: 420,
    borderRadius: 22,
    paddingHorizontal: 18,
    paddingVertical: 22,
    alignItems: "center",
    overflow: "hidden",
    borderWidth: 1,
    borderColor: "#F3F4F6",
  },
  emptyIconCircle: {
    width: 86,
    height: 86,
    borderRadius: 43,
    alignItems: "center",
    justifyContent: "center",
    marginBottom: 14,
  },
  emptyTitle: {
    fontSize: 18,
    fontFamily: FontFamily.serif,
    fontWeight: FontWeight.bold,
    textAlign: "center",
  },
  emptySubtitle: {
    marginTop: 6,
    fontSize: 14,
    fontFamily: FontFamily.serif,
    fontWeight: FontWeight.regular,
    textAlign: "center",
    lineHeight: 20,
  },
  emptyCta: {
    width: "100%",
    marginTop: 14,
  },
  content: {
    flex: 1,
  },
  profileSection: {
    alignItems: "center",
    paddingVertical: 32,
    backgroundColor: '#FFFFFF',
  },
  avatarWrapper: {
    position: 'relative',
    marginBottom: 16,
  },
  avatarCircle: {
    width: 96,
    height: 96,
    borderRadius: 48,
    alignItems: "center",
    justifyContent: "center",
  },
  editBadge: {
    position: 'absolute',
    bottom: 0,
    right: 0,
    width: 32,
    height: 32,
    borderRadius: 16,
    borderWidth: 3,
    alignItems: "center",
    justifyContent: "center",
  },
  avatarText: {
    fontSize: 36,
    fontFamily: FontFamily.serif,
    fontWeight: FontWeight.bold,
    color: "#0B6B3A",
    textTransform: "uppercase",
  },
  nameText: {
    fontSize: 22,
    fontFamily: FontFamily.serif,
    fontWeight: FontWeight.bold,
    marginBottom: 4,
    color: '#152036',
  },
  emailText: {
    fontSize: 15,
    fontFamily: FontFamily.serif,
    fontWeight: FontWeight.regular,
    color: '#6B7280',
  },
  sectionHeader: {
    paddingHorizontal: 20,
    paddingVertical: 12,
  },
  sectionHeaderText: {
    fontSize: 16,
    fontFamily: FontFamily.serif,
    fontWeight: FontWeight.regular,
    color: '#9CA3AF',
  },
  sectionBody: {
    borderTopWidth: 1,
    borderBottomWidth: 1,
    borderColor: '#F3F4F6',
  },
  menuItem: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingVertical: 16,
    paddingHorizontal: 20,
    borderBottomWidth: 1,
    borderBottomColor: '#F3F4F6',
  },
  menuItemLeft: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 16,
  },
  menuItemLabel: {
    justifyContent: 'center',
  },
  menuItemTitle: {
    fontSize: 16,
    fontFamily: FontFamily.serif,
    fontWeight: FontWeight.regular,
    color: '#152036',
  },
  menuItemSubtitle: {
    fontSize: 13,
    fontFamily: FontFamily.serif,
    fontWeight: FontWeight.regular,
    color: '#9CA3AF',
    marginTop: 2,
  },
  menuItemRight: {
    justifyContent: 'center',
    alignItems: 'center',
  },
});
