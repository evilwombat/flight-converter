/*
 *  Copyright (c) 2013, evilwombat
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 2 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Vector;

class TrackPoint
{
	double lat;
	double lon;
	double speed;
	int alt;
	int hour;
	int min;
	int sec;
	
	public TrackPoint() {
	}
	
	TrackPoint(double lat, double lon, int alt, double speed, int hour, int min, int sec) {
		this.lat = lat;
		this.lon = lon;
		this.speed = speed;
		this.alt = alt;
		this.hour = hour;
		this.min = min;
		this.sec = sec;
	}
	
	public String toString() {
		return String.format("%02d:%02d:%02d  speed: %.1f km/h  alt: %d   %.6f  %.6f", hour, min, sec, speed, alt, lat, lon);
	}
	
}

class Track
{
	public int day;
	public int month;
	public int year;
	public int num;
	Vector<TrackPoint> points = new Vector<TrackPoint>(0);
		
	public Track(int num) {
		this.num = num;
	}
	
	public Track(int month, int day, int year, int num)
	{
		this.day = day;
		this.month = month;
		this.year = year;
		this.num = num;
	}
	
	public String toString()
	{
		return "Track " + Integer.toString(num);
	}
}

class XMLOutput
{
	static void writeFileHeader(PrintWriter fd) {
		fd.format("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\" ?>\n");
		fd.format("<gpx xmlns=\"http://www.topografix.com/GPX/1/1\" xmlns:gpxx=\"http://www.garmin.com/xmlschemas/GpxExtensions/v3\" xmlns:gpxtpx=\"http://www.garmin.com/xmlschemas/TrackPointExtension/v1\" creator=\"Recon Instruments MOD / Flight HUD\" version=\"1.1\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd http://www.garmin.com/xmlschemas/GpxExtensions/v3 http://www.garmin.com/xmlschemas/GpxExtensionsv3.xsd http://www.garmin.com/xmlschemas/TrackPointExtension/v1 http://www.garmin.com/xmlschemas/TrackPointExtensionv1.xsd\">\n");
	};

	static void writeTrackHeader(PrintWriter fd, Track t) {
		fd.format("  <trk>\n");
		fd.format("    <name>Track %d</name>\n", t.num);
		fd.format("    <desc>%02d-%02d-%04d</desc>\n", t.month, t.day, t.year);
		fd.format("    <trkseg>\n");
	};
	
	static void writePoint(PrintWriter fd, Track t, TrackPoint pt)
	{
		fd.format("      <trkpt lat=\"%1.6f\" lon=\"%1.6f\">\n", pt.lat, pt.lon);
		fd.format("        <ele>%d</ele>\n", pt.alt);
		fd.format("        <name>%1.1f km/h</name>\n", pt.speed);
		fd.format("        <time>%04d-%02d-%02dT%02d:%02d:%02dZ</time>\n", t.year, t.month, t.day, pt.hour, pt.min, pt.sec);
		fd.format("      </trkpt>\n");
	}
	
	static void writeTrackFooter(PrintWriter fd) {
		fd.format("    </trkseg>\n");
		fd.format("  </trk>\n");
	};
	
	static void writeFileFooter(PrintWriter fd) {
		fd.format("</gpx>\n");
	};
	
	static void writeFile(String filename, Vector<Track> tracks) throws IOException {
		PrintWriter fd = new PrintWriter(new FileWriter(filename));
		writeFileHeader(fd);
		for (Track t : tracks) {
			writeTrackHeader(fd, t);
			
			for (TrackPoint pt : t.points)
				writePoint(fd, t, pt);
		
			writeTrackFooter(fd);
		}	
		writeFileFooter(fd);
		fd.close();
	}
}

public class FlightConverter
{
	static double decodeCoordinate(byte[] coord, int offset) {
		int b0, b1, b2, b3;
		double deg, min;
		
		b0 = coord[offset + 0] & 0xFF;
		b1 = coord[offset + 1] & 0xFF;
		b2 = coord[offset + 2] & 0xFF;
		b3 = coord[offset + 3] & 0xFF;
		
		deg = b0;
		min = (b1 & 0x7F) + b2 * 0.01f + b3 * 0.0001f;
		
		if ((b1 & 0x80) != 0)
			return -(deg + (min / 60.0f));
		else
			return deg + (min / 60.0f);
		
	}

	static int decodeHalfword(byte[] b, int offset) {
		/* Who the hell uses signed bytes, anyway? */
		return (((int) b[offset]) & 0xFF) * 256 + (((int) b[offset+1]) & 0xFF);
	}
	
	public static Vector<Track> parseFile(String filename) throws Exception {
		Vector<Track> tracks = new Vector<Track>(0);
		File inputFile = new File(filename);
		FileInputStream input = new FileInputStream(inputFile);
		
		byte[] header = new byte[9];
		byte[] log_entry = new byte[20];
		
		if (input.read(header) != 9)
			throw new Exception("Error reading header;");

		int len;
		int n_track = 0;

		Track curTrack = null;
		do { 
			len = input.read(log_entry);
			if (len != 20)
				break;
			
			/* Timestamp upper bit indicates new track */
			if ((log_entry[0] & 0x80) != 0) {
				int day, month, year;
				n_track++;

				System.out.println("Reading track " + n_track);
				
				year = 2000 + (log_entry[0] & 0x7F); 
				month = log_entry[1];
				day = log_entry[2];
				
				curTrack = new Track(month, day, year, n_track);
				tracks.addElement(curTrack);
				continue;
			}
			
			int hour, min, sec, alt;
			double lat, lon, speed;
			
			hour = log_entry[0];
			min = log_entry[1];
			sec = log_entry[2];
			
			lat = decodeCoordinate(log_entry, 3);
			lon = decodeCoordinate(log_entry, 7);
			
			speed = decodeHalfword(log_entry, 11) * 0.1f;
			alt = decodeHalfword(log_entry, 13);
			
			TrackPoint pt = new TrackPoint(lat, lon, alt, speed, hour, min, sec);
			
			if (curTrack == null) 
				throw new Exception("No current track - malformed file?");
			
			curTrack.points.addElement(pt);

		//	System.out.println(pt);
		
		} while (len == 20);
		
		if (tracks.size() != 0)
			return tracks;
		else
			return null;
	}
	
	public static void main(String[] args) throws Exception {
			System.out.println("\nRIB to GPX converter, version 0.1");
			System.out.println("This program is extremely beta - please be gentle");
			
			if (args.length == 0) {
				System.out.println("Usage: FlightConverter.jar <rib file> [more rib files...]");
				System.exit(-1);
			}
			
			for (String inFile : args) {
				System.out.println();
				System.out.println("Reading " + inFile);
				Vector<Track> tracks = null;
				
				try {
					tracks = parseFile(inFile);
				} catch (Exception e) {
					e.printStackTrace();					
				}
				
				if (tracks != null) {
					String outFile = inFile + ".gpx";
					System.out.println("Writing " + outFile);
					XMLOutput.writeFile(outFile, tracks);
				} else {
					System.out.println("No tracks found in "+ inFile);					
				}
			}
			System.out.println("Done");
		}
}