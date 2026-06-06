import SwiftUI

enum AuroraPalette {
    static let ink = Color(hex: 0x202933)
    static let ocean = Color(hex: 0x586B84)
    static let mist = Color(hex: 0xD7D8D5)
    static let linen = Color(hex: 0xF3ECE3)
    static let paper = Color(hex: 0xFAF5EE)
    static let graphite = Color(hex: 0x4F5964)
    static let auroraBlue = Color(hex: 0x7CC9FF)
    static let auroraTeal = Color(hex: 0x58D6C8)
    static let ember = Color(hex: 0xC97870)
    static let emberDeep = Color(hex: 0xB8655D)
    static let midnight = Color(hex: 0x07111B)
    static let island = Color(hex: 0x0D1824)
    static let deepSea = Color(hex: 0x142231)
    static let moonMist = Color(hex: 0x9EB2C6)
    static let frost = Color(hex: 0xF4F7FB)

    static func background(_ scheme: ColorScheme) -> Color {
        scheme == .dark ? midnight : linen
    }

    static func cardFill(_ scheme: ColorScheme) -> LinearGradient {
        if scheme == .dark {
            return LinearGradient(colors: [Color(hex: 0x132232), island], startPoint: .top, endPoint: .bottom)
        }
        return LinearGradient(colors: [Color(hex: 0xFBF7F1), Color(hex: 0xF4EEE6)], startPoint: .top, endPoint: .bottom)
    }

    static func heroFill(_ scheme: ColorScheme, active: Bool) -> LinearGradient {
        if scheme == .dark {
            return LinearGradient(
                colors: active ? [Color(hex: 0x16314B), Color(hex: 0x102437), Color(hex: 0x0A1825)] : [Color(hex: 0x74879C), Color(hex: 0x8494A7)],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
        }
        return LinearGradient(
            colors: active ? [Color(hex: 0x8FA0B4), Color(hex: 0x9EADBF), Color(hex: 0xAAB6C4)] : [Color(hex: 0xA8B1BC), Color(hex: 0xC0C8D0)],
            startPoint: .topLeading,
            endPoint: .bottomTrailing
        )
    }
}

extension Color {
    init(hex: UInt, alpha: Double = 1) {
        self.init(
            .sRGB,
            red: Double((hex >> 16) & 0xff) / 255,
            green: Double((hex >> 8) & 0xff) / 255,
            blue: Double(hex & 0xff) / 255,
            opacity: alpha
        )
    }
}

struct AuroraCard<Content: View>: View {
    @Environment(\.colorScheme) private var colorScheme
    var cornerRadius: CGFloat = 28
    var content: () -> Content

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            content()
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(AuroraPalette.cardFill(colorScheme))
        .clipShape(RoundedRectangle(cornerRadius: cornerRadius, style: .continuous))
        .overlay {
            RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
                .stroke(
                    LinearGradient(
                        colors: [
                            AuroraPalette.auroraBlue.opacity(colorScheme == .dark ? 0.34 : 0.18),
                            AuroraPalette.auroraTeal.opacity(colorScheme == .dark ? 0.18 : 0.10)
                        ],
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    ),
                    lineWidth: 1
                )
        }
    }
}

struct SectionHeader: View {
    var title: String

    var body: some View {
        HStack(spacing: 10) {
            RoundedRectangle(cornerRadius: 2)
                .fill(LinearGradient(colors: [AuroraPalette.auroraBlue, AuroraPalette.auroraTeal], startPoint: .top, endPoint: .bottom))
                .frame(width: 4, height: 16)
            Text(title.uppercased())
                .font(.caption.weight(.black))
                .foregroundStyle(AuroraPalette.graphite)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, 2)
        .padding(.vertical, 6)
    }
}

struct AuroraHeader: View {
    var onHistoryTap: () -> Void

    var body: some View {
        HStack {
            HStack(spacing: 12) {
                ZStack {
                    RoundedRectangle(cornerRadius: 16, style: .continuous)
                        .fill(LinearGradient(colors: [AuroraPalette.auroraBlue, AuroraPalette.auroraTeal], startPoint: .topLeading, endPoint: .bottomTrailing))
                    Image(systemName: "shield.lefthalf.filled")
                        .font(.title2.weight(.bold))
                        .foregroundStyle(.white)
                }
                .frame(width: 54, height: 54)

                Text("Aurora")
                    .font(.system(size: 34, weight: .black, design: .rounded))
                    .foregroundStyle(AuroraPalette.ocean)
            }

            Spacer()

            Button(action: onHistoryTap) {
                Image(systemName: "clock.arrow.circlepath")
                    .font(.system(size: 24, weight: .semibold))
                    .foregroundStyle(AuroraPalette.ocean)
                    .frame(width: 62, height: 62)
                    .background(Color.white.opacity(0.28))
                    .clipShape(Circle())
                    .overlay(Circle().stroke(AuroraPalette.mist.opacity(0.85), lineWidth: 1.2))
            }
            .accessibilityLabel("History")
        }
        .padding(.horizontal, 22)
        .padding(.top, 10)
        .padding(.bottom, 14)
    }
}

struct AuroraBottomTabBar: View {
    @Binding var selectedTab: AppTab

    var body: some View {
        HStack(spacing: 8) {
            ForEach(AppTab.allCases) { tab in
                Button {
                    selectedTab = tab
                } label: {
                    VStack(spacing: 6) {
                        Image(systemName: tab.systemImage)
                            .font(.system(size: 24, weight: .semibold))
                        Text(tab.title)
                            .font(.headline.weight(selectedTab == tab ? .black : .regular))
                    }
                    .foregroundStyle(selectedTab == tab ? AuroraPalette.ink : AuroraPalette.graphite)
                    .frame(maxWidth: .infinity)
                    .frame(height: 78)
                    .background {
                        if selectedTab == tab {
                            RoundedRectangle(cornerRadius: 30, style: .continuous)
                                .fill(Color(hex: 0xDDD5CB))
                                .shadow(color: .black.opacity(0.08), radius: 6, y: 2)
                        }
                    }
                }
                .buttonStyle(.plain)
                .accessibilityLabel(tab.title)
            }
        }
        .padding(6)
        .background(.ultraThinMaterial)
        .clipShape(RoundedRectangle(cornerRadius: 34, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 34, style: .continuous)
                .stroke(Color.white.opacity(0.55), lineWidth: 1)
        )
        .shadow(color: .black.opacity(0.08), radius: 12, y: 6)
        .padding(.horizontal, 18)
    }
}

struct StatusDot: View {
    var color: Color

    var body: some View {
        Circle()
            .fill(color)
            .frame(width: 10, height: 10)
    }
}

struct AuroraButtonStyle: ButtonStyle {
    var fill: Color = AuroraPalette.ocean
    var foreground: Color = .white

    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(.headline.weight(.bold))
            .foregroundStyle(foreground)
            .frame(maxWidth: .infinity)
            .frame(height: 50)
            .background(fill.opacity(configuration.isPressed ? 0.82 : 1))
            .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
    }
}

struct PermissionStateRow: View {
    var kind: PermissionKind
    var isGranted: Bool
    var detail: String
    var actionTitle: String
    var action: () -> Void

    var body: some View {
        HStack(spacing: 14) {
            ZStack {
                Circle()
                    .fill(isGranted ? AuroraPalette.auroraTeal.opacity(0.18) : AuroraPalette.ember.opacity(0.14))
                Image(systemName: kind.systemImage)
                    .foregroundStyle(isGranted ? AuroraPalette.auroraTeal : AuroraPalette.ember)
                    .font(.headline)
            }
            .frame(width: 44, height: 44)

            VStack(alignment: .leading, spacing: 3) {
                Text(kind.title)
                    .font(.headline.weight(.bold))
                    .foregroundStyle(AuroraPalette.ink)
                Text(detail)
                    .font(.subheadline)
                    .foregroundStyle(AuroraPalette.graphite)
                    .lineLimit(3)
            }

            Spacer()

            if !isGranted {
                Button(actionTitle, action: action)
                    .font(.caption.weight(.bold))
                    .buttonStyle(.bordered)
                    .controlSize(.small)
            } else {
                Image(systemName: "checkmark.square.fill")
                    .font(.title3)
                    .foregroundStyle(AuroraPalette.ocean)
            }
        }
        .padding(14)
        .background(Color.white.opacity(0.32))
        .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
    }
}

struct DeliveryStatusBadge: View {
    var status: TextAlertDeliveryStatus

    var body: some View {
        Text(status.label)
            .font(.caption.weight(.black))
            .foregroundStyle(color)
            .padding(.horizontal, 10)
            .padding(.vertical, 5)
            .background(color.opacity(0.13))
            .clipShape(Capsule())
    }

    private var color: Color {
        switch status {
        case .sent: AuroraPalette.auroraTeal
        case .failed: AuroraPalette.ember
        case .skipped: AuroraPalette.graphite
        }
    }
}
