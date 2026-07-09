import React from "react";
import { Platform, Pressable, ScrollView, StyleSheet, View } from "react-native";
import { useRouter } from "expo-router";
import MaterialIcons from "@expo/vector-icons/MaterialIcons";

import { ThemedText } from "@/components/themed-text";
import { ThemedView } from "@/components/themed-view";
import { PrimaryButton } from "@/components/ui/form-controls";
import { Colors } from "@/constants/theme";
import { useColorScheme } from "@/hooks/use-color-scheme";

export default function MenuScreen() {
  const theme = (useColorScheme() ?? "light") === "dark" ? "dark" : "light";
  const router = useRouter();

  const menuItems: Array<{ title: string; icon: any; route: string | null }> = [
    { title: "Sync Settings", icon: "sync", route: "/sync-settings" },
    { title: "Help & Support", icon: "help-outline", route: null },
    { title: "About Application", icon: "info-outline", route: null },
  ];

  return (
    <ThemedView style={styles.container}>
      <View style={[styles.header, { backgroundColor: Colors[theme].background }]}>
        <ThemedText type="title" style={styles.headerTitle}>More Options</ThemedText>
        <Pressable onPress={() => router.back()} style={({ pressed }) => [styles.closeBtn, pressed && { opacity: 0.7 }]}>
          <MaterialIcons name="close" size={28} color={Colors[theme].text} />
        </Pressable>
      </View>

      <ScrollView contentContainerStyle={styles.scrollContent}>
        <View style={[styles.menuContainer, { backgroundColor: Colors[theme].surface, borderColor: Colors[theme].border }]}>
          {menuItems.map((item, index) => (
            <React.Fragment key={index}>
              <Pressable
                style={({ pressed }) => [
                  styles.menuItem,
                  pressed && { opacity: 0.7 }
                ]}
                onPress={() => item.route && router.push(item.route as any)}
              >
                <View style={styles.menuItemLeft}>
                  <MaterialIcons name={item.icon} size={24} color={Colors[theme].muted} />
                  <ThemedText style={styles.menuItemText}>{item.title}</ThemedText>
                </View>
                <MaterialIcons name="chevron-right" size={24} color={Colors[theme].muted} />
              </Pressable>
              {index < menuItems.length - 1 && <View style={styles.divider} />}
            </React.Fragment>
          ))}
        </View>
      </ScrollView>
    </ThemedView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  header: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
    paddingHorizontal: 20,
    paddingTop: Platform.OS === "android" ? 40 : 60,
    paddingBottom: 20,
    borderBottomWidth: 1,
    borderBottomColor: "#E5E7EB",
  },
  headerTitle: {
    fontSize: 24,
  },
  closeBtn: {
    paddingHorizontal: 16,
    paddingVertical: 8,
    minHeight: 0,
  },
  scrollContent: {
    flexGrow: 1,
    padding: 20,
  },
  menuContainer: {
    borderRadius: 16,
    borderWidth: 1,
    overflow: "hidden",
  },
  menuItem: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
    paddingVertical: 16,
    paddingHorizontal: 16,
  },
  menuItemLeft: {
    flexDirection: "row",
    alignItems: "center",
    gap: 16,
  },
  menuItemText: {
    fontSize: 16,
  },
  divider: {
    height: 1,
    backgroundColor: "#F3F4F6",
    marginLeft: 56, // Align with text
  },
});
