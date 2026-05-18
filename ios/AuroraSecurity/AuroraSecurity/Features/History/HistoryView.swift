import SwiftUI

struct HistoryView: View {
    @EnvironmentObject private var appState: AppState

    var body: some View {
        List {
            if appState.historyRecords.isEmpty {
                ContentUnavailableView(
                    "No emergency history",
                    systemImage: "clock.badge.questionmark",
                    description: Text("SOS events and delivery outcomes will appear here.")
                )
                .listRowBackground(Color.clear)
            } else {
                ForEach(appState.historyRecords) { record in
                    NavigationLink {
                        HistoryDetailView(record: record)
                    } label: {
                        HistoryRow(record: record)
                    }
                    .listRowBackground(MorandiPalette.surface)
                }
            }
        }
        .scrollContentBackground(.hidden)
        .background(MorandiPalette.appBackground)
        .navigationTitle("History")
    }
}

private struct HistoryRow: View {
    var record: HistoryRecord

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack {
                Text(record.emergencyMode.title)
                    .font(.headline)
                Spacer()
                Text(record.deliveryStatus.displayName)
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(record.deliveryStatus == .sent ? MorandiPalette.sage : MorandiPalette.clay)
            }

            Text(record.triggerSource.title)
                .font(.subheadline)
                .foregroundStyle(MorandiPalette.mutedText)

            HStack {
                Text("\(Int(record.currentDb.rounded())) dB")
                Text(record.createdAt.formatted(date: .abbreviated, time: .shortened))
            }
            .font(.caption)
            .foregroundStyle(MorandiPalette.mutedText)
        }
        .padding(.vertical, 6)
    }
}

private struct HistoryDetailView: View {
    var record: HistoryRecord

    var body: some View {
        List {
            Section("Event") {
                LabeledContent("Mode", value: record.emergencyMode.title)
                LabeledContent("Source", value: record.triggerSource.title)
                LabeledContent("Sound level", value: "\(Int(record.currentDb.rounded())) dB")
                LabeledContent("Created", value: record.createdAt.formatted(date: .abbreviated, time: .standard))
            }

            Section("Alert") {
                Text(record.message)
                    .font(.callout)
                LabeledContent("Status", value: record.deliveryStatus.displayName)
            }

            if let location = record.location {
                Section("Location") {
                    if let mapsURL = URL(string: location.mapsURL) {
                        Link(location.mapsURL, destination: mapsURL)
                    } else {
                        Text(location.mapsURL)
                            .font(.caption)
                            .foregroundStyle(MorandiPalette.mutedText)
                    }
                }
            }

            if let audioFilePath = record.audioFilePath {
                Section("Audio") {
                    Text(audioFilePath)
                        .font(.caption)
                        .foregroundStyle(MorandiPalette.mutedText)
                }
            }

            if let aiSummary = record.aiSummary {
                Section("AI") {
                    Text(aiSummary)
                }
            }
        }
        .navigationTitle("Event")
    }
}
