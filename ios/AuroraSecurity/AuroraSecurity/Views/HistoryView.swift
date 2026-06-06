import AVFoundation
import SwiftUI

struct HistoryView: View {
    @Bindable var store: AuroraAppStore
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            ZStack {
                AuroraPalette.linen.ignoresSafeArea()
                VStack(spacing: 12) {
                    Picker("Filter", selection: $store.historyFilter) {
                        ForEach(HistoryFilter.allCases) { filter in
                            Text(filter.title).tag(filter)
                        }
                    }
                    .pickerStyle(.segmented)
                    .padding(.horizontal, 18)

                    if store.filteredHistoryRecords().isEmpty {
                        ContentUnavailableView(
                            "No history yet",
                            systemImage: "clock.arrow.circlepath",
                            description: Text("SOS events, incoming alerts, audio evidence, and AI summaries will appear here.")
                        )
                        .foregroundStyle(AuroraPalette.graphite)
                    } else {
                        ScrollView(showsIndicators: false) {
                            VStack(spacing: 18) {
                                ForEach(store.filteredHistoryRecords()) { record in
                                    HistoryRecordCard(record: record) {
                                        store.deleteHistoryRecord(record)
                                    }
                                }
                            }
                            .padding(.horizontal, 18)
                            .padding(.vertical, 10)
                        }
                    }
                }
            }
            .navigationTitle("History")
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button("Close") { dismiss() }
                }
                ToolbarItem(placement: .topBarTrailing) {
                    Button("Clear") {
                        store.clearHistory()
                    }
                    .disabled(store.historyRecords.isEmpty)
                }
            }
        }
    }
}

private struct HistoryRecordCard: View {
    var record: HistoryRecord
    var onDelete: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            BorderedHistorySection(title: "Alert Message", tint: AuroraPalette.emberDeep) {
                Text(primaryMessage)
                    .font(.title3)
                    .lineSpacing(5)
                    .foregroundStyle(AuroraPalette.ink)
                    .fixedSize(horizontal: false, vertical: true)
            }

            BorderedHistorySection(title: "Incident Details", tint: AuroraPalette.emberDeep) {
                VStack(alignment: .leading, spacing: 8) {
                    DetailRow(label: "Date:", value: record.sosDate.ifEmpty(formattedDate))
                    DetailRow(label: "Time:", value: record.sosTime.ifEmpty(formattedTime))
                    DetailRow(label: "Device:", value: record.sosDeviceName.ifEmpty("iPhone"))
                    DetailRow(label: "Mobile:", value: record.sosMobileNumber.ifEmpty("Not provided"))
                    DetailRow(label: "Sound:", value: record.sosCurrentSoundLevel.ifEmpty("Unknown"))
                    if let url = URL(string: record.sosLocationLink), !record.sosLocationLink.isEmpty {
                        HStack(alignment: .firstTextBaseline) {
                            Text("Location:")
                                .font(.headline.weight(.black))
                                .foregroundStyle(AuroraPalette.graphite)
                                .frame(width: 96, alignment: .leading)
                            Link("Google Maps", destination: url)
                                .font(.headline)
                        }
                    } else {
                        DetailRow(label: "Location:", value: record.sosLocationLabel.ifEmpty("Unavailable"))
                    }
                }
            }

            BorderedHistorySection(title: "Sound & AI Analysis", tint: AuroraPalette.emberDeep) {
                VStack(alignment: .leading, spacing: 14) {
                    if !record.audioFilePath.isEmpty {
                        AudioPlaybackButton(path: record.audioFilePath)
                    }
                    Divider()
                    Text(record.aiResultText.ifEmpty("No AI analysis recorded."))
                        .font(.body)
                        .foregroundStyle(AuroraPalette.graphite)
                        .lineSpacing(4)
                        .fixedSize(horizontal: false, vertical: true)
                }
            }

            if let summary = record.deliverySummary {
                HStack(spacing: 8) {
                    DeliveryStatusBadge(status: summary.sosAlertStatus)
                    DeliveryStatusBadge(status: summary.audioEvidenceStatus)
                    DeliveryStatusBadge(status: summary.aiSummaryAnalysisStatus)
                    Spacer()
                }
            }

            HStack {
                Label(record.type == .incomingPush ? "Incoming alert" : "Local alert", systemImage: record.type == .incomingPush ? "bell.badge" : "shield")
                    .font(.caption.weight(.bold))
                    .foregroundStyle(AuroraPalette.graphite)
                Spacer()
                Button(role: .destructive, action: onDelete) {
                    Image(systemName: "trash")
                }
            }
        }
        .padding(14)
        .background(Color(hex: 0xFFF7EF))
        .clipShape(RoundedRectangle(cornerRadius: 26, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 26, style: .continuous)
                .stroke(AuroraPalette.ember.opacity(0.28), lineWidth: 1)
        )
    }

    private var primaryMessage: String {
        record.sosMessage.ifEmpty(record.sosText.ifEmpty(record.notificationTitle.ifEmpty("SOS alert recorded.")))
    }

    private var formattedDate: String {
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd"
        return formatter.string(from: record.timestamp)
    }

    private var formattedTime: String {
        let formatter = DateFormatter()
        formatter.dateFormat = "HH:mm:ss"
        return formatter.string(from: record.timestamp)
    }
}

private struct BorderedHistorySection<Content: View>: View {
    var title: String
    var tint: Color
    var content: () -> Content

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(spacing: 10) {
                Circle()
                    .fill(tint)
                    .frame(width: 10, height: 10)
                Text(title.uppercased())
                    .font(.headline.weight(.black))
                    .foregroundStyle(tint)
            }
            content()
        }
        .padding(18)
        .frame(maxWidth: .infinity, alignment: .leading)
        .overlay(
            RoundedRectangle(cornerRadius: 24, style: .continuous)
                .stroke(tint.opacity(0.55), lineWidth: 1.2)
        )
    }
}

private struct DetailRow: View {
    var label: String
    var value: String

    var body: some View {
        HStack(alignment: .firstTextBaseline) {
            Text(label)
                .font(.headline.weight(.black))
                .foregroundStyle(AuroraPalette.graphite)
                .frame(width: 96, alignment: .leading)
            Text(value)
                .font(.headline)
                .foregroundStyle(AuroraPalette.ink)
                .fixedSize(horizontal: false, vertical: true)
        }
    }
}

private struct AudioPlaybackButton: View {
    var path: String
    @State private var player: AVAudioPlayer?
    @State private var isPlaying = false

    var body: some View {
        Button {
            togglePlayback()
        } label: {
            HStack(spacing: 14) {
                ZStack {
                    Circle()
                        .fill(AuroraPalette.mist.opacity(0.7))
                    Image(systemName: isPlaying ? "pause.fill" : "play.fill")
                        .foregroundStyle(AuroraPalette.ocean)
                }
                .frame(width: 52, height: 52)
                VStack(alignment: .leading, spacing: 2) {
                    Text("Audio Recording")
                        .font(.headline.weight(.black))
                        .foregroundStyle(AuroraPalette.ink)
                    Text(isPlaying ? "Playing" : "Tap to listen")
                        .foregroundStyle(AuroraPalette.graphite)
                }
                Spacer()
            }
            .padding(14)
            .background(AuroraPalette.mist.opacity(0.45))
            .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
        }
        .buttonStyle(.plain)
    }

    private func togglePlayback() {
        if isPlaying {
            player?.stop()
            isPlaying = false
            return
        }
        let url = URL(fileURLWithPath: path)
        player = try? AVAudioPlayer(contentsOf: url)
        player?.play()
        isPlaying = player?.isPlaying ?? false
    }
}

private extension String {
    func ifEmpty(_ fallback: String) -> String {
        isEmpty ? fallback : self
    }
}
