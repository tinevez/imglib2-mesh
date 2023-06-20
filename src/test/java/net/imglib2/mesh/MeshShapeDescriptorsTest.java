package net.imglib2.mesh;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealMatrixChangingVisitor;
import org.junit.BeforeClass;
import org.junit.Test;

import gnu.trove.list.array.TLongArrayList;
import net.imglib2.RealPoint;
import net.imglib2.mesh.alg.InertiaTensor;
import net.imglib2.mesh.alg.hull.ConvexHull;
import net.imglib2.mesh.obj.Mesh;
import net.imglib2.mesh.obj.naive.NaiveDoubleMesh;

public class MeshShapeDescriptorsTest
{

	private static final double EPSILON = 10e-12;

	private static Mesh mesh;

	private static NaiveDoubleMesh ch;

	@BeforeClass
	public static void setupBefore()
	{
		mesh = getMesh();
		ch = ConvexHull.calculate( mesh );
	}

	@Test
	public void volume()
	{
		final double actual = MeshShapeDescriptors.volume( mesh );
		// verified with matlab
		final double expected = 257.5000;
		assertEquals( "Incorrect volume for the mesh returned.", expected, actual, EPSILON );
	}

	@Test
	public void centroid()
	{
		// Computed with MATLAB.
		final double[] expected = new double[] { 5.812621359223301, 5.777346278317152, 4.818770226537216 };
		final RealPoint centroid = MeshShapeDescriptors.centroid( mesh );
		for ( int d = 0; d < expected.length; d++ )
		{
			assertEquals( "Incorrect centroid position returned for dimension " + d,
					expected[ d ], centroid.getDoublePosition( d ), EPSILON );
		}
	}

	@Test
	public void inertiaTensor()
	{
		// Computed with MATLAB.
		final double[] expected = new double[] {
				1562.29379719525, 295.17637540453, 22.2193365695803,
				295.17637540453, 1655.9349244876, 42.0405070118677,
				22.2193365695803, 42.0405070118677, 2061.54350053937
		};
		final RealMatrix it = InertiaTensor.calculate( mesh );
		it.walkInRowOrder( new RealMatrixChangingVisitor()
		{

			private int i = 0;

			@Override
			public double visit( final int row, final int column, final double value )
			{
				final double e = expected[ i++ ];
				assertEquals( "Incorrect inertia tensor value returned for row " + row + " and column " + column,
						e, value, EPSILON );
				return 0;
			}

			@Override
			public void start( final int rows, final int columns, final int startRow, final int endRow, final int startColumn, final int endColumn )
			{}

			@Override
			public double end()
			{
				return 0;
			}
		} );
	}

	@Test
	public void compactness()
	{
		final double actual = MeshShapeDescriptors.compactness( mesh );
		// formula verified and ground truth computed with matlab
		final double expected = 0.572416357359835;
		assertEquals( "Incorrect compactness returned.", expected, actual, EPSILON );
	}

	@Test
	public void convexityMesh()
	{
		final double actual = MeshShapeDescriptors.convexity( mesh, ch );
		// formula verified and ground truth computed with matlab
		final double expected = 0.983930494866521;
		assertEquals( "Incorrect convexity returned.", expected, actual, EPSILON );
	}

	@Test
	public void sizeConvexHullMesh()
	{
		final double actual = MeshShapeDescriptors.volume( ch );
		// ground truth computed with matlab
		final double expected = 304.5;
		assertEquals( "Incorrect convex hull volume returned.", expected, actual, EPSILON );
	}

	@Test
	public void sizeMesh()
	{
		final double actual = MeshShapeDescriptors.volume( mesh );
		// ground truth computed with matlab
		final double expected = 257.5;
		assertEquals( "Incorrect mesh volume returned.", expected, actual, EPSILON );
	}

	@Test
	public void solidityMesh()
	{
		final double actual = MeshShapeDescriptors.solidity( mesh, ch );
		// ground truth computed with matlab
		final double expected = 0.845648604269294;
		assertEquals( "Incorrect solidity returned.", expected, actual, EPSILON );
	}

	@Test
	public void sphericity()
	{
		final double actual = MeshShapeDescriptors.sphericity( mesh );
		// ground truth computed with matlab
		final double expected = 0.830304411183464;
		assertEquals( "Incorrect sphericity returned.", expected, actual, EPSILON );
	}

	@Test
	public void surfaceArea()
	{
		final double actual = MeshShapeDescriptors.surfaceArea( mesh );
		// ground truth computed with matlab
		final double expected = 235.7390893402464;
		assertEquals( "Incorrect surface area returned.", expected, actual, EPSILON );
	}

	@Test
	public void surfaceAreaConvexHull()
	{
		final double actual = MeshShapeDescriptors.surfaceArea( ch );
		// ground truth computed with matlab
		final double expected = 231.9508788339317;
		assertEquals( "Incorrect convex hull surface area returned.", expected, actual, EPSILON );
	}

	@Test
	public void verticesCountConvexHullMesh()
	{
		final long actual = ch.vertices().size();
		// verified with matlab
		final long expected = 57;
		assertEquals( "Incorrect number of vertices in the convex hull returned.", expected, actual, EPSILON );
	}

	@Test
	public void verticesCountMesh()
	{
		final long actual = mesh.vertices().size();
		// verified with matlab
		final long expected = 184;
		assertEquals( "Incorrect number of vertices in the mesh returned.", expected, actual, EPSILON );
	}

	public static Mesh getMesh()
	{
		final Mesh m = new NaiveDoubleMesh();
		// To prevent duplicates, map each (x, y, z) triple to its own index.
		final Map< Vector3D, Long > indexMap = new HashMap<>();
		final TLongArrayList indices = new TLongArrayList();
		try
		{
			Files.lines( Paths.get( MeshShapeDescriptorsTest.class.getResource( "3d_geometric_features_mesh.txt" ).toURI() ) )
					.forEach( l -> {
						final String[] coord = l.split( " " );
						final double x = Double.parseDouble( coord[ 0 ] );
						final double y = Double.parseDouble( coord[ 1 ] );
						final double z = Double.parseDouble( coord[ 2 ] );
						final Vector3D vertex = new Vector3D( x, y, z );
						final long vIndex = indexMap.computeIfAbsent( vertex, //
								v -> m.vertices().add( x, y, z ) );
						indices.add( vIndex );
					} );
		}
		catch ( IOException | URISyntaxException exc )
		{
			exc.printStackTrace();
		}
		for ( int i = 0; i < indices.size(); i += 3 )
		{
			final long v0 = indices.get( i );
			final long v1 = indices.get( i + 1 );
			final long v2 = indices.get( i + 2 );
			m.triangles().add( v0, v1, v2 );
		}
		return m;
	}
}
