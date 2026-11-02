package com.aaagrowers.scoutingapp.network;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.json.JSONObject;

public class ApiClient {

    /*
     * ============================================================
     * CONNECTION SETTINGS
     * ============================================================
     */

    private static final int CONNECT_TIMEOUT = 10000;
    private static final int READ_TIMEOUT = 15000;


    /*
     * ============================================================
     * LOGIN
     * ============================================================
     */

    public String login(
            String username,
            String password
    ) throws Exception {

        URL url =
                new URL(
                        ApiConfig.LOGIN_URL
                );


        HttpURLConnection connection =
                (HttpURLConnection) url.openConnection();


        try {

            /*
             * ----------------------------------------------------
             * REQUEST CONFIGURATION
             * ----------------------------------------------------
             */

            connection.setRequestMethod("POST");

            connection.setConnectTimeout(
                    CONNECT_TIMEOUT
            );

            connection.setReadTimeout(
                    READ_TIMEOUT
            );

            connection.setDoOutput(true);

            connection.setDoInput(true);

            connection.setRequestProperty(
                    "Content-Type",
                    "application/x-www-form-urlencoded; charset=UTF-8"
            );

            connection.setRequestProperty(
                    "Accept",
                    "application/json"
            );


            /*
             * ----------------------------------------------------
             * REQUEST BODY
             * ----------------------------------------------------
             */

            String requestBody =
                    "username="
                            +
                            URLEncoder.encode(
                                    username,
                                    StandardCharsets.UTF_8.name()
                            )
                            +
                            "&password="
                            +
                            URLEncoder.encode(
                                    password,
                                    StandardCharsets.UTF_8.name()
                            );


            byte[] requestBytes =
                    requestBody.getBytes(
                            StandardCharsets.UTF_8
                    );


            connection.setFixedLengthStreamingMode(
                    requestBytes.length
            );


            try (OutputStream outputStream =
                         connection.getOutputStream()) {

                outputStream.write(
                        requestBytes
                );

                outputStream.flush();
            }


            /*
             * ----------------------------------------------------
             * RESPONSE
             * ----------------------------------------------------
             */

            int responseCode =
                    connection.getResponseCode();


            InputStream inputStream;


            if (responseCode >= 200
                    &&
                    responseCode < 300) {

                inputStream =
                        connection.getInputStream();

            } else {

                inputStream =
                        connection.getErrorStream();
            }


            if (inputStream == null) {

                throw new Exception(
                        "Server returned HTTP "
                                + responseCode
                );
            }


            String response =
                    readResponse(
                            inputStream
                    );


            if (responseCode < 200
                    ||
                    responseCode >= 300) {

                throw new Exception(
                        "HTTP "
                                + responseCode
                                + ": "
                                + response
                );
            }


            return response;

        } finally {

            connection.disconnect();
        }
    }


    /*
     * ============================================================
     * READ RESPONSE
     * ============================================================
     */

    private String readResponse(
            InputStream inputStream
    ) throws Exception {

        StringBuilder response =
                new StringBuilder();


        try (
                BufferedReader reader =
                        new BufferedReader(
                                new InputStreamReader(
                                        inputStream,
                                        StandardCharsets.UTF_8
                                )
                        )
        ) {

            String line;


            while (
                    (line = reader.readLine())
                            != null
            ) {

                response.append(line);
            }
        }


        return response.toString();
    }

    /*
     * ============================================================
     * MASTER DATA
     * ============================================================
     */

    public String getMasterData() throws Exception {

        URL url =
                new URL(
                        ApiConfig.MASTER_DATA_URL
                );


        HttpURLConnection connection =
                (HttpURLConnection) url.openConnection();


        try {

            /*
             * ----------------------------------------------------
             * REQUEST CONFIGURATION
             * ----------------------------------------------------
             */

            connection.setRequestMethod("GET");

            connection.setConnectTimeout(
                    CONNECT_TIMEOUT
            );

            connection.setReadTimeout(
                    READ_TIMEOUT
            );

            connection.setDoInput(true);

            connection.setRequestProperty(
                    "Accept",
                    "application/json"
            );


            /*
             * ----------------------------------------------------
             * RESPONSE
             * ----------------------------------------------------
             */

            int responseCode =
                    connection.getResponseCode();


            InputStream inputStream;


            if (responseCode >= 200
                    &&
                    responseCode < 300) {

                inputStream =
                        connection.getInputStream();

            } else {

                inputStream =
                        connection.getErrorStream();
            }


            if (inputStream == null) {

                throw new Exception(
                        "Server returned HTTP "
                                + responseCode
                );
            }


            String response =
                    readResponse(
                            inputStream
                    );


            if (responseCode < 200
                    ||
                    responseCode >= 300) {

                throw new Exception(
                        "HTTP "
                                + responseCode
                                + ": "
                                + response
                );
            }


            return response;


        } finally {

            connection.disconnect();
        }
    }
    public String syncScoutingData(
            JSONObject payload
    ) throws Exception {

        URL url =
                new URL(
                        ApiConfig.SCOUTING_SYNC_URL
                );


        HttpURLConnection connection =
                (HttpURLConnection) url.openConnection();


        try {

            connection.setRequestMethod("POST");

            connection.setConnectTimeout(
                    CONNECT_TIMEOUT
            );

            connection.setReadTimeout(
                    READ_TIMEOUT
            );

            connection.setDoOutput(true);

            connection.setDoInput(true);

            connection.setRequestProperty(
                    "Content-Type",
                    "application/json; charset=UTF-8"
            );

            connection.setRequestProperty(
                    "Accept",
                    "application/json"
            );


            byte[] requestBytes =
                    payload
                            .toString()
                            .getBytes(
                                    StandardCharsets.UTF_8
                            );


            connection.setFixedLengthStreamingMode(
                    requestBytes.length
            );


            try (
                    OutputStream outputStream =
                            connection.getOutputStream()
            ) {

                outputStream.write(
                        requestBytes
                );

                outputStream.flush();
            }


            int responseCode =
                    connection.getResponseCode();


            InputStream inputStream;


            if (
                    responseCode >= 200
                            &&
                            responseCode < 300
            ) {

                inputStream =
                        connection.getInputStream();

            } else {

                inputStream =
                        connection.getErrorStream();
            }


            if (inputStream == null) {

                throw new Exception(
                        "Server returned HTTP "
                                + responseCode
                );
            }


            String response =
                    readResponse(
                            inputStream
                    );


            if (
                    responseCode < 200
                            ||
                            responseCode >= 300
            ) {

                throw new Exception(
                        "HTTP "
                                + responseCode
                                + ": "
                                + response
                );
            }


            return response;

        } finally {

            connection.disconnect();
        }
    }

    public JSONObject syncTrapObservations(
            JSONObject observation,
            File photoFile
    ) throws Exception {

        URL url =
                new URL(
                        ApiConfig.TRAP_OBSERVATION_SYNC_URL
                );

        HttpURLConnection connection =
                (HttpURLConnection) url.openConnection();

        String boundary =
                "----AAAGrowersBoundary"
                        + System.currentTimeMillis();

        try {

            connection.setRequestMethod("POST");

            connection.setConnectTimeout(
                    CONNECT_TIMEOUT
            );

            connection.setReadTimeout(
                    READ_TIMEOUT
            );

            connection.setDoOutput(true);

            connection.setDoInput(true);

            connection.setRequestProperty(
                    "Content-Type",
                    "multipart/form-data; boundary="
                            + boundary
            );

            connection.setRequestProperty(
                    "Accept",
                    "application/json"
            );


            try (
                    OutputStream outputStream =
                            connection.getOutputStream()
            ) {

                /*
                 * ========================================================
                 * OBSERVATION JSON
                 * ========================================================
                 */

                writeFormField(
                        outputStream,
                        boundary,
                        "observation",
                        observation.toString()
                );


                /*
                 * ========================================================
                 * PHOTO
                 * ========================================================
                 */

                if (
                        photoFile != null
                                &&
                                photoFile.exists()
                                &&
                                photoFile.length() > 0
                ) {

                    writeFileField(
                            outputStream,
                            boundary,
                            "photo",
                            photoFile
                    );
                }


                /*
                 * ========================================================
                 * END MULTIPART
                 * ========================================================
                 */

                outputStream.write(
                        ("--" + boundary + "--\r\n")
                                .getBytes(
                                        StandardCharsets.UTF_8
                                )
                );

                outputStream.flush();
            }


            /*
             * ============================================================
             * RESPONSE
             * ============================================================
             */

            int responseCode =
                    connection.getResponseCode();


            InputStream inputStream;


            if (
                    responseCode >= 200
                            &&
                            responseCode < 300
            ) {

                inputStream =
                        connection.getInputStream();

            } else {

                inputStream =
                        connection.getErrorStream();
            }


            if (inputStream == null) {

                throw new Exception(
                        "Server returned HTTP "
                                + responseCode
                );
            }


            String response =
                    readResponse(
                            inputStream
                    );


            JSONObject json =
                    new JSONObject(
                            response
                    );


            if (
                    responseCode < 200
                            ||
                            responseCode >= 300
            ) {

                throw new Exception(
                        json.optString(
                                "message",
                                "Server returned HTTP "
                                        + responseCode
                        )
                );
            }


            return json;


        } finally {

            connection.disconnect();
        }
    }

    private void writeFormField(
            OutputStream outputStream,
            String boundary,
            String fieldName,
            String value
    ) throws Exception {

        String header =
                "--"
                        + boundary
                        + "\r\n"
                        + "Content-Disposition: form-data; name=\""
                        + fieldName
                        + "\"\r\n"
                        + "Content-Type: application/json; charset=UTF-8\r\n"
                        + "\r\n";

        outputStream.write(
                header.getBytes(
                        StandardCharsets.UTF_8
                )
        );

        outputStream.write(
                value.getBytes(
                        StandardCharsets.UTF_8
                )
        );

        outputStream.write(
                "\r\n".getBytes(
                        StandardCharsets.UTF_8
                )
        );
    }


    private void writeFileField(
            OutputStream outputStream,
            String boundary,
            String fieldName,
            File file
    ) throws Exception {

        String fileName =
                file.getName();

        String header =
                "--"
                        + boundary
                        + "\r\n"
                        + "Content-Disposition: form-data; name=\""
                        + fieldName
                        + "\"; filename=\""
                        + fileName
                        + "\"\r\n"
                        + "Content-Type: image/jpeg\r\n"
                        + "\r\n";

        outputStream.write(
                header.getBytes(
                        StandardCharsets.UTF_8
                )
        );


        try (
                java.io.FileInputStream inputStream =
                        new java.io.FileInputStream(file)
        ) {

            byte[] buffer =
                    new byte[8192];

            int bytesRead;


            while (
                    (bytesRead =
                            inputStream.read(buffer))
                            != -1
            ) {

                outputStream.write(
                        buffer,
                        0,
                        bytesRead
                );
            }
        }


        outputStream.write(
                "\r\n".getBytes(
                        StandardCharsets.UTF_8
                )
        );
    }


}