##################################
# Stage 1: Build the Angular application
##################################
FROM node:20 AS build
# Set the working directory
WORKDIR /app
# Copy package.json and package-lock.json for dependency installation
COPY package*.json ./
# Install the dependencies
RUN npm install
# Copy the rest of the application files
COPY . .
# Build the Angular application for production
RUN npm run build --prod

##################################
# Stage 2: Serve the application with Nginx
##################################
FROM nginx:alpine
# Copy the built files from the previous stage
COPY --from=build /app/dist/factoryscada/browser /usr/share/nginx/html
# Expose the port that Nginx will use
EXPOSE 80
# Default command to run Nginx in the foreground
CMD ["nginx", "-g", "daemon off;"]
