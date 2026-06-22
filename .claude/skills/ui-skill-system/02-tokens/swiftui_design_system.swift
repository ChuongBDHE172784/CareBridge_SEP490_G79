import SwiftUI

/// MOMCARE CLAY DESIGN SYSTEM - SwiftUI Implementation
struct ClayColors {
    static let primary = Color(hex: "C98C7B")
    static let primaryHover = Color(hex: "B67868")
    static let background = Color(hex: "F6F1EC")
    static let surface = Color(hex: "F2EAE4")
    static let card = Color.white
    static let textPrimary = Color(hex: "5A463F")
    static let textSecondary = Color(hex: "9C857C")
}

struct ClayShadow: ViewModifier {
    var radius: CGFloat = 24
    var y: CGFloat = 12
    var opacity: Double = 0.06
    
    func body(content: Content) -> some View {
        content.shadow(color: Color(hex: "5A463F").opacity(opacity), radius: radius, x: 0, y: y)
    }
}

extension View {
    func clayShadow(floating: bool = false) -> some View {
        if floating {
            return self.modifier(ClayShadow(radius: 64, y: 24, opacity: 0.12))
        }
        return self.modifier(ClayShadow())
    }
}

struct ClayCard<Content: View>: View {
    let content: Content
    
    init(@ViewBuilder content: () -> Content) {
        self.content = content()
    }
    
    var body: some View {
        content
            .padding(24)
            .background(ClayColors.card)
            .cornerRadius(32)
            .clayShadow()
            .overlay(
                RoundedRectangle(cornerRadius: 32)
                    .stroke(ClayColors.surface.opacity(0.5), lineWidth: 1)
            )
    }
}

// Hex Extension Helper
extension Color {
    init(hex: String) {
        let hex = hex.trimmingCharacters(in: CharacterSet.alphanumerics.inverted)
        var int: UInt64 = 0
        Scanner(string: hex).scanHexInt64(&int)
        let a, r, g, b: UInt64
        switch hex.count {
        case 3: (a, r, g, b) = (255, (int >> 8) * 17, (int >> 4 & 0xF) * 17, (int & 0xF) * 17)
        case 6: (a, r, g, b) = (255, int >> 16, int >> 8 & 0xFF, int & 0xFF)
        default: (a, r, g, b) = (1, 1, 1, 0)
        }
        self.init(.sRGB, red: Double(r) / 255, green: Double(g) / 255, blue:  Double(b) / 255, opacity: Double(a) / 255)
    }
}
