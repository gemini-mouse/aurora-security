import SwiftUI

enum MorandiPalette {
    static let appBackground = Color(red: 0.94, green: 0.92, blue: 0.88)
    static let surface = Color(red: 0.98, green: 0.96, blue: 0.92)
    static let elevatedSurface = Color(red: 0.90, green: 0.89, blue: 0.84)
    static let sage = Color(red: 0.48, green: 0.58, blue: 0.51)
    static let mistBlue = Color(red: 0.53, green: 0.62, blue: 0.67)
    static let clay = Color(red: 0.72, green: 0.59, blue: 0.54)
    static let mauve = Color(red: 0.68, green: 0.57, blue: 0.62)
    static let graphite = Color(red: 0.19, green: 0.20, blue: 0.20)
    static let mutedText = Color(red: 0.43, green: 0.43, blue: 0.40)
    static let danger = Color(red: 0.67, green: 0.30, blue: 0.28)
}

struct AuroraPanel: ViewModifier {
    func body(content: Content) -> some View {
        content
            .padding(16)
            .background(MorandiPalette.surface)
            .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
            .shadow(color: .black.opacity(0.06), radius: 12, x: 0, y: 6)
    }
}

extension View {
    func auroraPanel() -> some View {
        modifier(AuroraPanel())
    }
}
