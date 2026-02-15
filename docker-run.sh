#!/bin/bash

# Customer Statement Processor - Docker Helper Script

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print colored messages
print_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

# Function to check if Docker is running
check_docker() {
    if ! docker info > /dev/null 2>&1; then
        print_error "Docker is not running. Please start Docker and try again."
        exit 1
    fi
}

# Function to build the Docker image
build() {
    print_info "Building Docker image..."
    docker-compose build
    print_info "Build completed successfully!"
}

# Function to start the application
start() {
    print_info "Starting Customer Statement Processor..."
    docker-compose up -d
    print_info "Application started!"
    print_info "Waiting for application to be ready..."
    sleep 5
    status
}

# Function to stop the application
stop() {
    print_info "Stopping Customer Statement Processor..."
    docker-compose down
    print_info "Application stopped!"
}

# Function to restart the application
restart() {
    print_info "Restarting Customer Statement Processor..."
    stop
    start
}

# Function to show logs
logs() {
    print_info "Showing logs (Ctrl+C to exit)..."
    docker-compose logs -f customer-statement-processor
}

# Function to show status
status() {
    print_info "Application Status:"
    docker-compose ps
    echo ""
    
    # Check if container is running
    if docker-compose ps | grep -q "Up"; then
        print_info "Health Check:"
        sleep 2
        if curl -s http://localhost:8080/actuator/health > /dev/null 2>&1; then
            print_info "✓ Application is healthy"
            echo ""
            print_info "Access Points:"
            echo "  - API Endpoint: http://localhost:8080/api/v1/statements/validate"
            echo "  - Swagger UI: http://localhost:8080/swagger-ui.html"
            echo "  - API Docs: http://localhost:8080/api-docs"
            echo "  - Health Check: http://localhost:8080/actuator/health"
        else
            print_warning "Application is starting up..."
        fi
    else
        print_warning "Application is not running"
    fi
}

# Function to test the API
test() {
    print_info "Testing API with sample files..."
    
    if [ ! -f "test-data/records.csv" ]; then
        print_error "test-data/records.csv not found"
        exit 1
    fi
    
    print_info "Testing CSV upload..."
    curl -X POST http://localhost:8080/api/v1/statements/validate \
        -F "file=@test-data/records.csv" \
        -H "Content-Type: multipart/form-data" \
        -w "\n" | jq '.' || echo ""
    
    if [ -f "test-data/records.json" ]; then
        print_info "Testing JSON upload..."
        curl -X POST http://localhost:8080/api/v1/statements/validate \
            -F "file=@test-data/records.json" \
            -H "Content-Type: multipart/form-data" \
            -w "\n" | jq '.' || echo ""
    fi
}

# Function to clean up
clean() {
    print_warning "This will remove all containers, images, and volumes. Continue? (y/N)"
    read -r response
    if [[ "$response" =~ ^([yY][eE][sS]|[yY])$ ]]; then
        print_info "Cleaning up..."
        docker-compose down -v
        docker rmi customer-statement-processor:latest 2>/dev/null || true
        print_info "Cleanup completed!"
    else
        print_info "Cleanup cancelled"
    fi
}

# Function to show help
show_help() {
    cat << EOF
Customer Statement Processor - Docker Helper Script

Usage: ./docker-run.sh [COMMAND]

Commands:
    build       Build the Docker image
    start       Start the application
    stop        Stop the application
    restart     Restart the application
    logs        Show application logs (follow mode)
    status      Show application status and health
    test        Test the API with sample files
    clean       Remove all containers, images, and volumes
    help        Show this help message

Examples:
    ./docker-run.sh build       # Build the image
    ./docker-run.sh start       # Start the application
    ./docker-run.sh logs        # View logs
    ./docker-run.sh test        # Test the API

EOF
}

# Main script logic
check_docker

case "${1:-help}" in
    build)
        build
        ;;
    start)
        start
        ;;
    stop)
        stop
        ;;
    restart)
        restart
        ;;
    logs)
        logs
        ;;
    status)
        status
        ;;
    test)
        test
        ;;
    clean)
        clean
        ;;
    help|--help|-h)
        show_help
        ;;
    *)
        print_error "Unknown command: $1"
        echo ""
        show_help
        exit 1
        ;;
esac
