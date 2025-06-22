// swift-tools-version:5.3
import PackageDescription

let package = Package(
    name: "Gateway",
    platforms: [
        .iOS(.v14),
    ],
    products: [
        .library(
            name: "Gateway",
            targets: ["Gateway"]
        )
    ],
    targets: [
        .binaryTarget(
            name: "Gateway",
            url: "https://github.com/brahyam/gateway-kmp/releases/download/v0.1.0/Gateway.xcframework.zip",
            checksum: "971e3c147157f0c90e57286e208abc1f7e38d5645049dc53e9051bcb5644efcb"
        )
    ]
)
