package com.example.smsexpensetracker

//class MainActivity : AppCompatActivity() {
//
//    private val READ_SMS_PERMISSION_REQUEST = 100
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_main)
//
//        // this function will start when the app open.
//
//        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
//            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_SMS), READ_SMS_PERMISSION_REQUEST)
//        } else {
//            // Permission already granted, proceed with reading SMS
//            readSMSMessages()
//        }
//    }
//
//    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
//        if (requestCode == READ_SMS_PERMISSION_REQUEST) {
//            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                // Permission granted, proceed with reading SMS
//                readSMSMessages()
//            } else {
//                // Permission denied, handle accordingly (e.g., show a message)
//            }
//        }
//    }
//
//    private fun readSMSMessages() {
//        Log.d("SMS", "readSMSMessages() called") // Add this line
//        val bankNumbers = arrayOf("+918500542535", "BANK_SENDER_ID")
//        val selection = "address IN (${bankNumbers.joinToString(", ") { "'$it'" }})"
//
//        val cursor = contentResolver.query(
//            Uri.parse("content://sms/inbox"),
//            arrayOf("_id", "address", "date", "body"),
//            selection,
//            null,
//            "date DESC"
//        )
//
//        if (cursor != null) {
//            Log.d("SMS", "Cursor is not null") // Add this line
//            if (cursor.moveToFirst()) {
//                Log.d("SMS", "Cursor moved to first") // Add this line
//                do {
//                    val address = cursor.getString(cursor.getColumnIndexOrThrow("address"))
//                    val body = cursor.getString(cursor.getColumnIndexOrThrow("body"))
//
//                    Log.d("SMS", "From: $address")
//                    Log.d("SMS", "Message: $body")
//
//                    // Parse the SMS body using regex (example)
//                    val amountRegex = Regex("(?i)debited\\s+INR\\s*([\\d,.]+)"); // Example regex, adjust as needed.
//                    val amountMatch = amountRegex.find(body);
//
//                    if (amountMatch != null){
//                        val amount = amountMatch.groupValues[1].replace(",", "");
//                        Log.d("SMS", "Amount: $amount");
//                    } else {
//                        Log.d("SMS", "Amount: Not Found");
//                    }
//
//                } while (cursor.moveToNext())
//            }
//            cursor.close()
//        } else {
//            Log.d("SMS", "Cursor is null") // Add this line
//        }
//    }
//}