package com.example.hifienhancer

data class Preset(val name: String, val eq: List<Float>, val bass: Int, val virt: Int, val loud: Int)

object Presets {
    // name | 5-point EQ curve in dB (low -> high) | bass 0-1000 | 3D 0-1000 | loudness mB
    private const val RAW = """
Flat|0,0,0,0,0|0|0|0
Rock|5,3,-2,3,5|400|300|300
Pop|-1,3,5,3,-1|200|300|300
Jazz|3,2,-2,2,4|100|200|200
Classical|4,2,-1,3,4|0|300|100
Hip Hop|6,4,0,2,3|700|200|400
EDM|6,3,0,3,6|800|500|500
Dance|6,4,1,3,4|700|400|400
Bass Heavy|8,5,0,0,0|900|100|300
Treble Boost|0,0,0,5,8|0|200|200
Vocal|-2,1,5,4,1|0|100|200
Acoustic|4,3,2,3,3|100|200|100
Metal|5,2,-3,4,6|500|300|400
Country|3,2,1,3,3|200|200|200
Blues|3,2,0,2,3|200|200|200
R&B|5,4,1,2,3|600|200|300
Reggae|5,2,-1,1,3|600|200|200
Latin|4,3,1,3,4|400|300|300
Lounge|-2,1,3,1,-1|100|400|100
Folk|3,2,1,2,2|100|200|100
Techno|6,4,0,2,5|800|500|500
House|6,3,1,2,4|700|400|400
Trance|5,3,1,3,6|600|600|400
Dubstep|8,5,-1,2,4|1000|400|500
Drum and Bass|7,4,0,3,5|800|300|500
Punk|3,2,2,4,4|300|200|400
Indie|2,2,2,3,3|200|300|200
Soul|4,3,2,2,2|400|200|200
Funk|5,3,1,3,3|500|200|300
Opera|2,1,3,4,3|0|400|100
Piano|1,1,2,4,4|0|200|100
Guitar|2,2,3,4,3|100|200|200
Podcast|-4,1,5,4,0|0|0|300
Audiobook|-5,0,5,3,-1|0|0|300
Movie|5,2,1,3,4|500|700|400
Gaming|4,2,2,5,5|400|700|400
Night Mode|-3,0,3,2,-2|0|100|600
Small Speaker|6,4,2,2,1|600|100|500
Earbuds|4,2,0,2,3|300|200|200
Open Headphones|3,1,0,1,2|200|200|100
Closed Headphones|-1,0,1,2,3|0|200|100
Studio Monitor|0,0,0,0,0|0|0|0
Hi-Fi Reference|1,0,0,1,2|0|0|0
Hi-Fi Warm|3,2,0,-1,-1|100|0|0
Hi-Fi Bright|-1,0,1,3,4|0|0|0
Hi-Fi Analytical|-2,0,1,2,2|0|0|0
Live Concert|3,2,2,4,4|300|700|300
Club|7,4,0,3,5|800|500|500
Stadium|4,2,1,3,4|400|900|400
Cinema Dialog|-1,1,5,3,1|200|500|300
Bollywood|4,3,3,3,3|400|300|300
Qawwali|2,3,4,3,2|300|300|200
Ghazal|2,2,4,3,1|200|300|100
Lo-Fi|3,2,0,-3,-5|200|0|100
Chill|2,1,0,1,1|200|400|100
Workout|6,3,1,3,5|700|300|600
Party|7,4,1,3,5|800|500|600
Car Audio|5,3,0,3,5|600|400|400
Karaoke|0,1,4,2,0|0|300|300
Deep Bass|7,3,-2,-1,0|900|0|200
Soft Bass|3,2,0,0,0|300|0|100
Bright Vocal|-2,0,4,5,3|0|100|200
Warm Vocal|-1,2,4,1,-1|100|100|200
Clear Highs|0,0,1,4,6|0|200|100
Loud and Clear|4,2,1,4,5|400|300|800
Sleep|1,1,-1,-3,-5|0|0|0
Focus|-1,0,2,1,0|0|0|0
"""
    val all: List<Preset> = RAW.trim().lines().map { line ->
        val p = line.split("|")
        Preset(p[0], p[1].split(",").map { it.toFloat() }, p[2].toInt(), p[3].toInt(), p[4].toInt())
    }

    // Stretch the 5-point curve across however many bands the phone has
    fun bandGain(curve: List<Float>, band: Int, bands: Int): Float {
        if (bands <= 1) return curve[0]
        val pos = band.toFloat() / (bands - 1) * (curve.size - 1)
        val lo = pos.toInt().coerceAtMost(curve.size - 2)
        val frac = pos - lo
        return curve[lo] + (curve[lo + 1] - curve[lo]) * frac
    }
}
