#!/bin/bash

# Build XCFramework and setup Swift Package Manager
# This script builds the Gateway XCFramework and sets up Swift Package Manager support

set -e

echo "🚀 Building Gateway XCFramework and setting up Swift Package Manager..."

# Check if we're in the right directory
if [ ! -f "gateway-client/build.gradle.kts" ]; then
    echo "❌ Please run this script from the gateway-kmp root directory"
    exit 1
fi

# Read version from gradle.properties
if [ ! -f "gradle.properties" ]; then
    echo "❌ gradle.properties not found"
    exit 1
fi

VERSION_NAME=$(grep "^VERSION_NAME=" gradle.properties | cut -d'=' -f2)
if [ -z "$VERSION_NAME" ]; then
    echo "❌ VERSION_NAME not found in gradle.properties"
    exit 1
fi

echo "📋 Using version: $VERSION_NAME"

# Parse argument: local or release
MODE=${1:-release}

if [[ "$MODE" != "local" && "$MODE" != "release" ]]; then
    echo "❌ Usage: $0 [local|release]"
    exit 1
fi

# Build the XCFramework
echo "📦 Building XCFramework..."
./gradlew :gateway-client:assembleGatewayXCFramework

# Check if build was successful
if [ $? -ne 0 ]; then
    echo "❌ Failed to build XCFramework"
    exit 1
fi

# Define paths
FRAMEWORK_PATH="gateway-client/build/XCFrameworks/release/Gateway.xcframework"
ZIP_PATH="Gateway.xcframework.zip"

echo "✅ XCFramework built successfully at: $FRAMEWORK_PATH"

if [ "$MODE" == "local" ]; then
    echo "🛠  Local mode: copying unzipped XCFramework to root and updating Package.swift for local path."
    # Remove any existing framework in root
    if [ -d "Gateway.xcframework" ]; then
        rm -rf "Gateway.xcframework"
    fi
    cp -R "$FRAMEWORK_PATH" ./Gateway.xcframework
    echo "📝 Creating Package.swift in root directory (local path)..."
    cat > "Package.swift" << EOF
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
            path: "./Gateway.xcframework"
        )
    ]
)
EOF
    echo "✅ Local Swift Package setup complete!"
    exit 0
fi

# Create ZIP archive
echo "📦 Creating ZIP archive..."
if [ -f "$ZIP_PATH" ]; then
    rm "$ZIP_PATH"
fi
zip -r "$ZIP_PATH" "$FRAMEWORK_PATH"

# Calculate checksum
echo "🔍 Calculating checksum..."
CHECKSUM=$(swift package compute-checksum "$ZIP_PATH")
echo "📋 Checksum: $CHECKSUM"

# Create Package.swift in root directory
echo "📝 Creating Package.swift in root directory..."
cat > "Package.swift" << EOF
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
            url: "https://github.com/brahyam/Gateway/releases/download/$VERSION_NAME/Gateway.xcframework.zip",
            checksum: "$CHECKSUM"
        )
    ]
)
EOF

# Update .gitignore to exclude build artifacts but keep Package.swift
echo "🔧 Updating .gitignore..."
if ! grep -q "Package.swift" .gitignore; then
    echo "" >> .gitignore
    echo "# Swift Package Manager" >> .gitignore
    echo "# Keep Package.swift but ignore build artifacts" >> .gitignore
    echo "*.xcodeproj/" >> .gitignore
    echo "*.xcworkspace/" >> .gitignore
    echo ".build/" >> .gitignore
    echo "DerivedData/" >> .gitignore
fi

echo "✅ Swift Package setup complete!"
echo ""
echo "📋 Next steps:"
echo "1. Upload $ZIP_PATH to GitHub releases"
echo "2. Update the URL in Package.swift with the actual release URL"
echo "3. Commit and push: git add Package.swift .gitignore && git commit -m 'Add Swift Package Manager support'"
echo "4. Create and push a tag: git tag $VERSION_NAME && git push origin $VERSION_NAME"
echo ""
echo "🔗 Users can then add the package using:"
echo "   https://github.com/brahyam/Gateway.git"
echo ""
echo "🔢 Checksum: $CHECKSUM" 