import processing.core.*; 
import processing.data.*; 
import processing.event.*; 
import processing.opengl.*; 

import java.util.HashMap; 
import java.util.ArrayList; 
import java.io.File; 
import java.io.BufferedReader; 
import java.io.PrintWriter; 
import java.io.InputStream; 
import java.io.OutputStream; 
import java.io.IOException; 

public class trees extends PApplet {

float growthFrac = 0.02f;
float leafGrowthFrac = 0.01f;

float minBranchRadius;
float maxBranchRadius;
float minBranchStartFrac = 0.25f;
float maxBranchStartFrac = 0.8f;
float newBranchProb = 0.04f;
int maxBranchLevel = 4;

float coverageEpsilon = 0.15f;

float minLeafStartFrac = 0.65f;
float maxLeafStartFrac = 0.97f;
float newLeafProb = 0.09f;

class Vec2 {
  float x, y;
  public Vec2(float x, float y) {
    this.x = x; this.y = y;
  }
  public Vec2 extendBy(float mag) {
    float theta = atan2(y, x);
    float magnitude = sqrt(pow(x, 2) + pow(y, 2)) + mag;
    return new Vec2(magnitude * cos(theta), magnitude * sin(theta));
  }
  public Vec2 offsetBy(float ext, float angle) {
    return new Vec2(x + ext * cos(angle), y + ext * sin(angle));
  }
  public Vec2 plus(Vec2 other) {
    return new Vec2(x + other.x, y + other.y);
  }
  public Vec2 minus(Vec2 other) {
    return new Vec2(x - other.x, y - other.y);
  }
}

class Mat2 {
  private float[][] vals;
  public Mat2(float rotationAngle) {
    vals = new float[][]{{cos(rotationAngle), -sin(rotationAngle)},
                         {sin(rotationAngle), cos(rotationAngle)}};
  }
  public Vec2 times(Vec2 v) {
    return new Vec2(vals[0][0] * v.x + vals[0][1] * v.y, vals[1][0] * v.x + vals[1][1] * v.y);
  }
}

class Leaf {
  PImage image;
  public float frac;
  
  float angle;
  float finalAngle = random(-HALF_PI/2, HALF_PI/2);
  
  int alpha = 0;
  int finalAlpha = PApplet.parseInt(random(140, 230));
  
  int sideLen = 1;
  int finalSideLen;
  
  public Leaf(float angle, float frac) {
    println(width);
    this.angle = angle;
    this.frac = frac;
    this.finalAngle += angle;
    finalSideLen = PApplet.parseInt(random(width/57, width/22));
    this.image = loadImage("dim-" + String.format("%02d", PApplet.parseInt(random(1, 51))) + ".png");
  }
  
  public void drawAt(Vec2 drawPoint) {
    pushMatrix();
    pushStyle();
    translate(drawPoint.x, drawPoint.y);
    rotate(angle);
    tint(255, alpha);
    image(image, 0, 0, sideLen, sideLen);
    popStyle();
    popMatrix();
    
    alpha += (finalAlpha - alpha) * leafGrowthFrac;
    angle += (finalAngle - angle) * leafGrowthFrac;
    sideLen += ceil((finalSideLen - sideLen) * leafGrowthFrac);    
  }
}

class Branch {
  int dir;
  float radius;
  float startAngle;
  
  float currentAngleCoverage;
  float totalAngleCoverage;
  
  int level;
  
  ArrayList<Branch> branches = new ArrayList<Branch>();
  ArrayList<Float> branchFracs = new ArrayList<Float>();
  ArrayList<Leaf> leaves = new ArrayList<Leaf>();
  
  public Branch(int dir, float radius, float startAngle, float totalAngleCoverage, int level) {
    this.dir = dir;
    this.radius = dir * radius;
    this.startAngle = startAngle;
    this.currentAngleCoverage = 0;
    this.totalAngleCoverage = dir * totalAngleCoverage;
    this.level = level;
  }
  
  private void addBranch() {
    branches.add(randomBranch(startAngle + currentAngleCoverage, level + 1));
    branchFracs.add(random(minBranchStartFrac, maxBranchStartFrac));
  }
  
  private void addLeaf() {
    leaves.add(new Leaf(startAngle + currentAngleCoverage, random(minLeafStartFrac, maxLeafStartFrac)));
  }
  
  public void drawFrom(Vec2 start) {
    Vec2 center = start.offsetBy(radius, startAngle);
    Vec2 end = new Mat2(currentAngleCoverage).times(start.minus(center)).plus(center);
    //drawDebugPoints(start, end, center);
        
    noFill();
    stroke(color(255, 0, 255 * PApplet.parseFloat(level-1)/maxBranchLevel), 150);
    strokeWeight(2);
    
    if (dir > 0) {
      arc(center.x, center.y, 2 * radius, 2 * radius, startAngle + PI, startAngle + currentAngleCoverage + PI);  
    } else {
      arc(center.x, center.y, -2 * radius, -2 * radius, startAngle + currentAngleCoverage, startAngle);
    }
    
    currentAngleCoverage += growthFrac * (totalAngleCoverage - currentAngleCoverage);
    
    if (abs((totalAngleCoverage - currentAngleCoverage) / totalAngleCoverage) > coverageEpsilon) {
      if (level < maxBranchLevel && random(1) < newBranchProb/level) {
        addBranch();
      }
      if (random(1) < newLeafProb) {
        addLeaf();
      }
    }
    
    for (Branch b : branches) {
      float frac = branchFracs.get(branches.indexOf(b));
      Vec2 drawPoint = new Mat2(dir * frac * abs(currentAngleCoverage/totalAngleCoverage)).times(start.minus(center)).plus(center);
      b.drawFrom(drawPoint);
    }
    
    for (Leaf l : leaves) {
      Vec2 drawPoint = new Mat2(dir * l.frac * abs(currentAngleCoverage/totalAngleCoverage)).times(start.minus(center)).plus(center);
      l.drawAt(drawPoint);
    }
  }
}

public void drawDebugPoints(Vec2 start, Vec2 end, Vec2 center) {
  float r = 10;
  
  pushStyle();
  noStroke();
    
  fill(0xff00FF00);
  ellipse(start.x, start.y, r, r);
  
  fill(0xff0000FF);
  ellipse(center.x, center.y, r, r);
  
  fill(0xffFF0000);
  ellipse(end.x, end.y, r, r);
    
  pushStyle();
  fill(20, 20, 20, 10);
  
  popStyle();
}

public Branch randomBranch(float startAngle, int level) {
  int dir = random(1) > 0.5f ? 1 : -1;
  return new Branch(dir, sqrt(PApplet.parseFloat(maxBranchLevel - (level - 1))/maxBranchLevel) * random(minBranchRadius, maxBranchRadius), startAngle, random(QUARTER_PI, HALF_PI), level);
}

Branch root;

public void setup() {
  
  // =========== Modify the window size here! ===========
  
  //size(1000, 700);
  // ====================================================
  
  
  
  minBranchRadius = width * 1/5.f;
  maxBranchRadius = width * 1/2.f;
  
  reset();
}

public void reset() {
  background(0xffEEEEEE);
  root = randomBranch(0, 1);
}

public void draw() {
  background(0xffEEEEEE);
  root.drawFrom(new Vec2(width/2, height));
}

public void keyPressed() {
  if (key == ' ') {
    reset();
  }
}
  public void settings() {  fullScreen(P2D);  smooth(); }
  static public void main(String[] passedArgs) {
    String[] appletArgs = new String[] { "trees" };
    if (passedArgs != null) {
      PApplet.main(concat(appletArgs, passedArgs));
    } else {
      PApplet.main(appletArgs);
    }
  }
}
