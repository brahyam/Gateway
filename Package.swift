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
            url: "https://github.com/brahyam/gateway-kmp/releases/download/0.1.1/Gateway.xcframework.zip",
            checksum: "b4eb139bc492336eb79444927de56cb4c13517684d629637f99b186c16df0d20"
        )
    ]
)
